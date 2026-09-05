package com.example.network

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Event hierarchy emitted across the real-time telemetry stream pipeline.
 */
sealed class TelemetryStreamEvent {
  data class ConnectionStateChanged(
    val oldStatus: ConnectionStatus,
    val newStatus: ConnectionStatus,
    val message: String
  ) : TelemetryStreamEvent()

  data class Reconnecting(
    val attempt: Int,
    val maxAttempts: Int,
    val delayMs: Long,
    val targetUrl: String
  ) : TelemetryStreamEvent()

  data class RfFrameReceived(val frame: RFSpectrumFrame) : TelemetryStreamEvent()
  data class FsoFrameReceived(val frame: FSOSpectrumFrame) : TelemetryStreamEvent()
  data class DetectionEventReceived(val event: DetectionEvent) : TelemetryStreamEvent()
  data class SpatialEmittersReceived(val emitters: List<SpatialEmitter>) : TelemetryStreamEvent()
  data class ModulationClassified(val classification: ModulationClassification) : TelemetryStreamEvent()
  data class DecodedFramesReceived(val frames: List<DecodedFrame>) : TelemetryStreamEvent()
  data class LatencyMeasured(val latencyMs: Long) : TelemetryStreamEvent()
  data class ServerStatusUpdated(val isScanning: Boolean, val fsoAllBlocked: Boolean) : TelemetryStreamEvent()
  data class PacketFetchCompleted(val pipeline: String, val timestamp: Long) : TelemetryStreamEvent()
  data class RawMessageReceived(val payload: String) : TelemetryStreamEvent()
  data class ErrorOccurred(val error: String, val isFatal: Boolean) : TelemetryStreamEvent()
}

/**
 * Configuration options for WebSocketManager reconnection and heartbeat policies.
 */
data class WebSocketConfig(
  val autoReconnect: Boolean = true,
  val initialReconnectDelayMs: Long = 1500L,
  val maxReconnectDelayMs: Long = 30000L,
  val backoffMultiplier: Double = 1.8,
  val maxReconnectAttempts: Int = 12,
  val pingIntervalSeconds: Long = 3L,
  val readTimeoutMs: Long = 0L,
  val userAgent: String = "UnifiedSignalShield/2.5 (Android; Military-SIGINT-Client)"
)

/**
 * WebSocketManager handles the real-time telemetry stream connection,
 * auto-reconnect logic with exponential backoff & jitter, and event-driven
 * data handling pipelines for RF and FSO signal streams.
 */
class WebSocketManager(
  private val scope: CoroutineScope,
  val context: Context? = null,
  private val config: WebSocketConfig = WebSocketConfig()
) {
  private val tag = "WebSocketManager"

  companion object {
    const val DEFAULT_PORT = 8765
    const val EMULATOR_HOST = "10.0.2.2"
    const val PHYSICAL_DEVICE_ADB_LOOPBACK = "127.0.0.1"

    /**
     * Computes the proper default Base URL according to the current runtime environment:
     * - On Android Emulator: ws://10.0.2.2:8765
     * - On Physical Android Device: ws://<machine_local_ip>:8765 or ws://127.0.0.1:8765 (ADB Reverse)
     */
    fun getDefaultBaseUrl(context: Context? = null, port: Int = DEFAULT_PORT): String {
      val isEmulator = com.example.util.NetworkUtils.isRunningOnEmulator()
      if (isEmulator) {
        return "ws://$EMULATOR_HOST:$port"
      }

      val localDeviceIp = com.example.util.NetworkUtils.getDeviceLocalIpAddress(context)
      if (localDeviceIp != null) {
        val subnetPrefix = com.example.util.NetworkUtils.getSubnetPrefix(localDeviceIp)
        return "ws://${subnetPrefix}100:$port"
      }
      return "ws://$PHYSICAL_DEVICE_ADB_LOOPBACK:$port"
    }
  }

  private val okHttpClient = OkHttpClient.Builder()
    .readTimeout(config.readTimeoutMs, TimeUnit.MILLISECONDS)
    .connectTimeout(15, TimeUnit.SECONDS)
    .pingInterval(15, TimeUnit.SECONDS)
    .retryOnConnectionFailure(true)
    .build()

  private var webSocket: WebSocket? = null
  private var isConnecting = false
  private var isManuallyDisconnected = false

  // Reconnect & Heartbeat jobs
  private var reconnectJob: Job? = null
  private var pingJob: Job? = null

  // Target endpoint cache
  private var currentHost: String = if (com.example.util.NetworkUtils.isRunningOnEmulator()) EMULATOR_HOST else PHYSICAL_DEVICE_ADB_LOOPBACK
  private var currentPort: Int = DEFAULT_PORT
  private var currentServerName: String = ""
  private var activeFullUrl: String = ""

  // StateFlows for UI and Subsystems
  private val _connectionStatus = MutableStateFlow(ConnectionStatus.SIMULATION)
  val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

  private val _activeServerUrl = MutableStateFlow("")
  val activeServerUrl: StateFlow<String> = _activeServerUrl.asStateFlow()

  private val _serverDescription = MutableStateFlow("")
  val serverDescription: StateFlow<String> = _serverDescription.asStateFlow()

  private val _lastLogMessage = MutableStateFlow("Simulation Engine Ready")
  val lastLogMessage: StateFlow<String> = _lastLogMessage.asStateFlow()

  private val _latencyMs = MutableStateFlow(0L)
  val latencyMs: StateFlow<Long> = _latencyMs.asStateFlow()

  private val _reconnectAttempt = MutableStateFlow(0)
  val reconnectAttempt: StateFlow<Int> = _reconnectAttempt.asStateFlow()

  private val _isReconnecting = MutableStateFlow(false)
  val isReconnecting: StateFlow<Boolean> = _isReconnecting.asStateFlow()

  private val _isServerScanning = MutableStateFlow(true)
  val isServerScanning: StateFlow<Boolean> = _isServerScanning.asStateFlow()

  private val _fsoAllBlocked = MutableStateFlow(false)
  val fsoAllBlocked: StateFlow<Boolean> = _fsoAllBlocked.asStateFlow()

  // Pipelines: RF and FSO Signal Data Streams
  private val _rfFrame = MutableStateFlow(RFSpectrumFrame())
  val rfFrame: StateFlow<RFSpectrumFrame> = _rfFrame.asStateFlow()

  private val _fsoFrame = MutableStateFlow(FSOSpectrumFrame())
  val fsoFrame: StateFlow<FSOSpectrumFrame> = _fsoFrame.asStateFlow()

  private val _spatialEmitters = MutableStateFlow<List<SpatialEmitter>>(emptyList())
  val spatialEmitters: StateFlow<List<SpatialEmitter>> = _spatialEmitters.asStateFlow()

  private val _modulationClassification = MutableStateFlow(
    ModulationClassification(
      modulation = "1024-QAM (802.11ax)",
      confidence = 0.94f,
      snrEstDb = 28.5,
      baudRateEstKhz = 80000.0,
      cyclostationaryPeakAlpha = 40.0
    )
  )
  val modulationClassification: StateFlow<ModulationClassification> = _modulationClassification.asStateFlow()

  private val _decodedFrames = MutableStateFlow<List<DecodedFrame>>(emptyList())
  val decodedFrames: StateFlow<List<DecodedFrame>> = _decodedFrames.asStateFlow()

  // Event-driven broadcast pipelines
  private val _telemetryEvents = MutableSharedFlow<TelemetryStreamEvent>(
    replay = 0,
    extraBufferCapacity = 128
  )
  val telemetryEvents: SharedFlow<TelemetryStreamEvent> = _telemetryEvents.asSharedFlow()

  private val _detectionEvents = MutableSharedFlow<DetectionEvent>(
    replay = 0,
    extraBufferCapacity = 64
  )
  val detectionEvents: SharedFlow<DetectionEvent> = _detectionEvents.asSharedFlow()

  /**
   * Checks whether android.permission.INTERNET is granted in the application manifest.
   */
  fun checkInternetPermission(): Boolean {
    val ctx = context ?: return true
    return ContextCompat.checkSelfPermission(
      ctx,
      Manifest.permission.INTERNET
    ) == PackageManager.PERMISSION_GRANTED
  }

  /**
   * Checks whether the device currently has an active network link (Wi-Fi, Cellular, Ethernet, or Loopback).
   */
  fun checkNetworkConnected(): Boolean {
    val ctx = context ?: return true
    return try {
      val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
      val network = cm?.activeNetwork ?: return false
      val caps = cm.getNetworkCapabilities(network) ?: return false
      caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } catch (e: Exception) {
      true
    }
  }

  /**
   * Connects to a WebSocket stream host / URL.
   */
  fun connect(
    host: String = getDefaultBaseUrl(context, DEFAULT_PORT),
    port: Int = DEFAULT_PORT,
    serverName: String = "",
    enableAutoReconnect: Boolean = true
  ) {
    if (isConnecting) return
    isManuallyDisconnected = false

    cancelReconnectJob()

    if (_connectionStatus.value == ConnectionStatus.CONNECTED) {
      disconnectInternal(notifyUser = false)
    }

    val url = resolveWebSocketUrl(host, port)
    currentHost = host
    currentPort = port
    currentServerName = serverName
    activeFullUrl = url
    _activeServerUrl.value = url
    _serverDescription.value = if (serverName.isNotEmpty()) {
      serverName
    } else if (url.contains("8765") || url.contains("gnuradio")) {
      "GNU Radio Live Flowgraph Sink"
    } else if (url.contains("9001") || url.contains("grc")) {
      "GNU Radio Companion Spectrum Bridge"
    } else {
      "GNU Radio & SDR Telemetry Stream"
    }

    initiateConnection(url, isRetry = false)
  }

  /**
   * Disconnects the active socket and cancels auto-reconnect.
   */
  fun disconnect() {
    isManuallyDisconnected = true
    cancelReconnectJob()
    disconnectInternal(notifyUser = true)
    _connectionStatus.value = ConnectionStatus.DISCONNECTED
    _isReconnecting.value = false
    _reconnectAttempt.value = 0
    _lastLogMessage.value = "Disconnected by user"
  }

  /**
   * Switches to local synthetic simulation mode.
   */
  fun switchToSimulation() {
    disconnect()
    _connectionStatus.value = ConnectionStatus.SIMULATION
    _lastLogMessage.value = "Active Mode: GNU Radio Emulation Engine"
    emitTelemetryEvent(
      TelemetryStreamEvent.ConnectionStateChanged(
        oldStatus = _connectionStatus.value,
        newStatus = ConnectionStatus.SIMULATION,
        message = "Switched to GNU Radio emulation mode"
      )
    )
  }

  /**
   * Resolves raw host or full URL to a valid ws:// or wss:// format.
   * Replaces bare 'localhost' with machine IP or loopback depending on the runtime platform:
   * - On Android Emulator: maps 'localhost' and '127.0.0.1' -> '10.0.2.2'
   * - On Physical Device: maps 'localhost' -> '127.0.0.1' (for ADB reverse) or preserves host IP.
   */
  fun resolveWebSocketUrl(host: String, port: Int = DEFAULT_PORT): String {
    var cleanHost = host.trim()
    if (cleanHost.isEmpty()) {
      return getDefaultBaseUrl(context, port)
    }

    val cleanPort = if (port > 0) port.toString().trim() else DEFAULT_PORT.toString()
    val isEmulator = com.example.util.NetworkUtils.isRunningOnEmulator()

    if (isEmulator) {
      // Map localhost/127.0.0.1 to 10.0.2.2 ONLY on Android emulator
      if (cleanHost.startsWith("ws://localhost") || cleanHost.startsWith("ws://127.0.0.1")) {
        cleanHost = cleanHost.replace("localhost", EMULATOR_HOST).replace("127.0.0.1", EMULATOR_HOST)
      } else if (cleanHost.startsWith("http://localhost") || cleanHost.startsWith("http://127.0.0.1")) {
        cleanHost = cleanHost.replace("localhost", EMULATOR_HOST).replace("127.0.0.1", EMULATOR_HOST)
      } else if (cleanHost == "localhost" || cleanHost == "127.0.0.1") {
        cleanHost = EMULATOR_HOST
      }
    } else {
      // On physical device, replace 'localhost' with '127.0.0.1' for ADB reverse compatibility
      if (cleanHost.startsWith("ws://localhost")) {
        cleanHost = cleanHost.replace("localhost", PHYSICAL_DEVICE_ADB_LOOPBACK)
      } else if (cleanHost.startsWith("http://localhost")) {
        cleanHost = cleanHost.replace("localhost", PHYSICAL_DEVICE_ADB_LOOPBACK)
      } else if (cleanHost == "localhost") {
        cleanHost = PHYSICAL_DEVICE_ADB_LOOPBACK
      }
    }

    return when {
      cleanHost.startsWith("ws://") || cleanHost.startsWith("wss://") -> cleanHost
      cleanHost.startsWith("http://") -> cleanHost.replace("http://", "ws://")
      cleanHost.startsWith("https://") -> cleanHost.replace("https://", "wss://")
      cleanHost.contains(":") -> "ws://$cleanHost"
      cleanPort == "443" || cleanHost.contains("websocket.events") -> "wss://$cleanHost"
      else -> "ws://$cleanHost:$cleanPort"
    }
  }

  /**
   * Probes whether a specific host and TCP port is reachable via raw socket.
   */
  suspend fun probeTcpPort(targetHostOrUrl: String, targetPort: Int = DEFAULT_PORT, timeoutMs: Int = 2500): String = withContext(Dispatchers.IO) {
    try {
      var host = targetHostOrUrl.trim()
        .removePrefix("ws://")
        .removePrefix("wss://")
        .removePrefix("http://")
        .removePrefix("https://")

      var port = targetPort
      if (host.contains(":")) {
        val parts = host.split(":")
        host = parts[0]
        port = parts[1].toIntOrNull() ?: targetPort
      }

      val isEmulator = com.example.util.NetworkUtils.isRunningOnEmulator()
      if (!isEmulator && host == EMULATOR_HOST) {
        return@withContext "ERROR: 10.0.2.2 is an emulator IP. On a physical device, use your PC's Wi-Fi IP or 'adb reverse tcp:$port tcp:$port' (127.0.0.1)."
      }

      val socket = java.net.Socket()
      socket.connect(java.net.InetSocketAddress(host, port), timeoutMs)
      socket.close()
      "SUCCESS: TCP port $port on $host is OPEN & ready for WebSocket!"
    } catch (e: java.net.ConnectException) {
      val isLoopback = targetHostOrUrl.contains(PHYSICAL_DEVICE_ADB_LOOPBACK) || targetHostOrUrl.contains("localhost")
      if (isLoopback && !com.example.util.NetworkUtils.isRunningOnEmulator()) {
        "REFUSED: 127.0.0.1:$targetPort refused. Run 'adb reverse tcp:$targetPort tcp:$targetPort' in PC terminal & start GNU Radio."
      } else {
        "REFUSED: Target host is reachable but port $targetPort is not listening. Ensure GNU Radio Python script is running."
      }
    } catch (e: java.net.SocketTimeoutException) {
      "TIMEOUT: No response from $targetHostOrUrl in ${timeoutMs}ms. Check Wi-Fi connection & PC firewall."
    } catch (e: java.net.UnknownHostException) {
      "NOT FOUND: Unknown host '$targetHostOrUrl'. Check IP address format."
    } catch (e: Exception) {
      "PROBE ERROR: ${e.message ?: e.javaClass.simpleName}"
    }
  }

  /**
   * Internal connection trigger with security & permission validations.
   */
  private fun initiateConnection(url: String, isRetry: Boolean) {
    // 1. Check android.permission.INTERNET manifest declaration
    if (!checkInternetPermission()) {
      isConnecting = false
      _connectionStatus.value = ConnectionStatus.DISCONNECTED
      val msg = "Security Error: Missing android.permission.INTERNET in AndroidManifest.xml"
      _lastLogMessage.value = msg
      Log.e(tag, msg)
      emitTelemetryEvent(TelemetryStreamEvent.ErrorOccurred(msg, isFatal = true))
      return
    }

    // 2. Check active network connectivity
    if (!checkNetworkConnected()) {
      Log.w(tag, "Active network not available or disconnected")
      _lastLogMessage.value = "Warning: No active network connection detected"
    }

    isConnecting = true
    val oldStatus = _connectionStatus.value
    _connectionStatus.value = ConnectionStatus.CONNECTING
    _lastLogMessage.value = if (isRetry) {
      "Reconnecting to $url (Attempt ${_reconnectAttempt.value}/${config.maxReconnectAttempts})..."
    } else {
      "Connecting to $url..."
    }

    emitTelemetryEvent(
      TelemetryStreamEvent.ConnectionStateChanged(
        oldStatus = oldStatus,
        newStatus = ConnectionStatus.CONNECTING,
        message = _lastLogMessage.value
      )
    )

    Log.i(tag, "Initiating WebSocket connection to: $url (retry=$isRetry, cleartextConfig=enabled)")
    val request = try {
      Request.Builder()
        .url(url)
        .addHeader("User-Agent", config.userAgent)
        .build()
    } catch (e: Exception) {
      Log.e(tag, "Invalid WebSocket URL: $url", e)
      isConnecting = false
      _connectionStatus.value = ConnectionStatus.DISCONNECTED
      _lastLogMessage.value = "Invalid URL format: $url"
      emitTelemetryEvent(TelemetryStreamEvent.ErrorOccurred("Invalid URL format: ${e.message}", isFatal = true))
      return
    }

    webSocket = okHttpClient.newWebSocket(request, createWebSocketListener(url))
  }

  private fun createWebSocketListener(url: String): WebSocketListener {
    return object : WebSocketListener() {
      override fun onOpen(ws: WebSocket, response: Response) {
        Log.i(tag, "WebSocket stream established: $url")
        isConnecting = false
        _isReconnecting.value = false
        _reconnectAttempt.value = 0

        val oldStatus = _connectionStatus.value
        _connectionStatus.value = ConnectionStatus.CONNECTED
        val serverHost = response.request.url.host
        _lastLogMessage.value = "Connected to $serverHost [HTTP ${response.code}]"

        emitTelemetryEvent(
          TelemetryStreamEvent.ConnectionStateChanged(
            oldStatus = oldStatus,
            newStatus = ConnectionStatus.CONNECTED,
            message = _lastLogMessage.value
          )
        )

        // Send telemetry subscription handshake
        sendSubscriptionHandshake(ws)

        // Start periodic ping loop
        startPingHeartbeat()
      }

      override fun onMessage(ws: WebSocket, text: String) {
        _lastLogMessage.value = "Rx ${text.length} bytes"
        emitTelemetryEvent(TelemetryStreamEvent.RawMessageReceived(text))
        processIncomingTelemetry(text)
      }

      override fun onClosing(ws: WebSocket, code: Int, reason: String) {
        Log.i(tag, "WebSocket closing ($code): $reason")
        _lastLogMessage.value = "Closing ($code): $reason"
        ws.close(1000, null)
      }

      override fun onClosed(ws: WebSocket, code: Int, reason: String) {
        Log.i(tag, "WebSocket closed ($code): $reason")
        handleDisconnectOrFailure("Connection closed ($code): $reason", isFailure = false)
      }

      override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
        val rawMsg = t.message ?: "Socket connection error"
        val isEmulator = com.example.util.NetworkUtils.isRunningOnEmulator()
        val is10022 = url.contains("10.0.2.2")
        val isLoopback = url.contains("127.0.0.1") || url.contains("localhost")

        val actionableMsg = when {
          !isEmulator && is10022 ->
            "Failure: 10.0.2.2 only works on emulators! On physical Android, use Wi-Fi IP (e.g. 192.168.1.X) or run 'adb reverse tcp:8765 tcp:8765' (127.0.0.1)."
          !isEmulator && isLoopback && (t is java.net.ConnectException || rawMsg.contains("ECONNREFUSED", ignoreCase = true) || rawMsg.contains("Failed to connect", ignoreCase = true)) ->
            "Failure: 127.0.0.1 connection refused. Did you run 'adb reverse tcp:8765 tcp:8765' in terminal on your PC & start GNU Radio?"
          t is java.net.ConnectException || rawMsg.contains("ECONNREFUSED", ignoreCase = true) ->
            "Failure: Connection refused at $url. Ensure GNU Radio Python script is running and listening on 0.0.0.0."
          t is java.net.SocketTimeoutException || rawMsg.contains("ETIMEDOUT", ignoreCase = true) ->
            "Failure: Timeout connecting to $url. Check that phone & PC are on SAME Wi-Fi and PC firewall allows incoming port."
          t is java.net.NoRouteToHostException || rawMsg.contains("EHOSTUNREACH", ignoreCase = true) ->
            "Failure: Host unreachable. Verify your computer's local IP address (e.g. via 'ipconfig')."
          t is java.net.UnknownHostException ->
            "Failure: Unknown host '${rawMsg}'. Check hostname / IP address format."
          else ->
            "Failure: $rawMsg"
        }

        Log.w(tag, "WebSocket failure: $actionableMsg (raw: $rawMsg)")
        emitTelemetryEvent(TelemetryStreamEvent.ErrorOccurred(actionableMsg, isFatal = false))
        handleDisconnectOrFailure(actionableMsg, isFailure = true)
      }
    }
  }

  /**
   * Handles disconnect or failure and triggers auto-reconnect if enabled.
   */
  private fun handleDisconnectOrFailure(message: String, isFailure: Boolean) {
    isConnecting = false
    stopPingHeartbeat()
    disconnectInternal(notifyUser = false)

    val oldStatus = _connectionStatus.value
    _connectionStatus.value = ConnectionStatus.DISCONNECTED
    _lastLogMessage.value = message

    emitTelemetryEvent(
      TelemetryStreamEvent.ConnectionStateChanged(
        oldStatus = oldStatus,
        newStatus = ConnectionStatus.DISCONNECTED,
        message = message
      )
    )

    if (config.autoReconnect && !isManuallyDisconnected && activeFullUrl.isNotEmpty()) {
      scheduleAutoReconnect()
    }
  }

  /**
   * Auto-Reconnect logic with exponential backoff and jitter.
   */
  private fun scheduleAutoReconnect() {
    val currentAttempt = _reconnectAttempt.value + 1
    if (currentAttempt > config.maxReconnectAttempts) {
      Log.w(tag, "Maximum reconnect attempts (${config.maxReconnectAttempts}) reached. Halting auto-reconnect.")
      _isReconnecting.value = false
      _lastLogMessage.value = "Auto-reconnect stopped: max attempts reached."
      return
    }

    _reconnectAttempt.value = currentAttempt
    _isReconnecting.value = true

    // Calculate exponential backoff with jitter
    val rawDelay = (config.initialReconnectDelayMs * Math.pow(config.backoffMultiplier, (currentAttempt - 1).toDouble())).toLong()
    val cappedDelay = min(rawDelay, config.maxReconnectDelayMs)
    val jitter = Random.nextLong(100, 500)
    val finalDelayMs = cappedDelay + jitter

    Log.i(tag, "Scheduling auto-reconnect attempt $currentAttempt/${config.maxReconnectAttempts} in ${finalDelayMs}ms")
    _lastLogMessage.value = "Reconnecting in ${(finalDelayMs / 1000.0).format(1)}s (Attempt $currentAttempt/${config.maxReconnectAttempts})..."

    emitTelemetryEvent(
      TelemetryStreamEvent.Reconnecting(
        attempt = currentAttempt,
        maxAttempts = config.maxReconnectAttempts,
        delayMs = finalDelayMs,
        targetUrl = activeFullUrl
      )
    )

    cancelReconnectJob()
    reconnectJob = scope.launch {
      delay(finalDelayMs)
      if (isActive && !isManuallyDisconnected && _connectionStatus.value != ConnectionStatus.CONNECTED) {
        initiateConnection(activeFullUrl, isRetry = true)
      }
    }
  }

  private fun cancelReconnectJob() {
    reconnectJob?.cancel()
    reconnectJob = null
  }

  private fun disconnectInternal(notifyUser: Boolean) {
    stopPingHeartbeat()
    try {
      webSocket?.close(1000, "Client disconnect")
    } catch (e: Exception) {
      Log.w(tag, "Error closing socket: ${e.message}")
    }
    webSocket = null
    isConnecting = false
  }

  /**
   * Sends the initial telemetry subscription handshake JSON.
   */
  private fun sendSubscriptionHandshake(ws: WebSocket) {
    val handshake = JSONObject().apply {
      put("cmd", "subscribe")
      put("client", "UnifiedSignalShield-v3.0")
      put("origin", "gnuradio_sdr_telemetry_client")
      put("timestamp", System.currentTimeMillis())
      put("capabilities", JSONArray(listOf("RF_SPECTRUM", "FSO_OPTICAL", "MUSIC_AOA", "RADIO_ML", "PROTOCOL_DISSECT")))
      put("subscriptions", JSONArray(listOf("rf_spectrum", "fso_optical", "music_aoa", "radioml_amr", "decoded_frames", "events")))
    }
    ws.send(handshake.toString())
  }

  /**
   * Heartbeat / Ping loop to track stream latency and keep socket alive.
   */
  private fun startPingHeartbeat() {
    stopPingHeartbeat()
    pingJob = scope.launch {
      while (isActive && _connectionStatus.value == ConnectionStatus.CONNECTED) {
        sendPing()
        delay(config.pingIntervalSeconds * 1000L)
      }
    }
  }

  private fun stopPingHeartbeat() {
    pingJob?.cancel()
    pingJob = null
  }

  /**
   * Event-driven processing of incoming WebSocket telemetry payloads.
   */
  private fun processIncomingTelemetry(text: String) {
    try {
      val json = JSONObject(text)
      val type = json.optString("type", "").lowercase()

      when (type) {
        "pong" -> handlePong(json)
        "rf", "rf_spectrum", "rf_packet", "rf_frame" -> {
          handleRfFrame(json)
          emitTelemetryEvent(TelemetryStreamEvent.PacketFetchCompleted("RF", System.currentTimeMillis()))
        }
        "fso", "fso_optical", "fso_packet", "fso_frame" -> {
          handleFsoFrame(json)
          emitTelemetryEvent(TelemetryStreamEvent.PacketFetchCompleted("FSO", System.currentTimeMillis()))
        }
        "telemetry_poll", "telemetry_poll_response", "poll_response" -> {
          val data = json.optJSONObject("data") ?: json
          if (data.has("rf") || data.has("rf_packet") || data.has("rf_frame")) {
            handleRfFrame(data.optJSONObject("rf") ?: data.optJSONObject("rf_packet") ?: data.optJSONObject("rf_frame") ?: data)
            emitTelemetryEvent(TelemetryStreamEvent.PacketFetchCompleted("RF", System.currentTimeMillis()))
          }
          if (data.has("fso") || data.has("fso_packet") || data.has("fso_frame")) {
            handleFsoFrame(data.optJSONObject("fso") ?: data.optJSONObject("fso_packet") ?: data.optJSONObject("fso_frame") ?: data)
            emitTelemetryEvent(TelemetryStreamEvent.PacketFetchCompleted("FSO", System.currentTimeMillis()))
          }
        }
        "event", "alarm", "threat" -> handleDetectionEvent(json)
        "aoa", "music_aoa", "spatial" -> handleSpatialEmitters(json)
        "radioml", "modulation", "amr" -> handleModulationClassification(json)
        "frames", "dissector", "packet" -> handleDecodedFrames(json)
        "status" -> handleServerStatus(json)
      }
    } catch (e: Exception) {
      Log.e(tag, "Error parsing incoming telemetry JSON: ${e.message}")
    }
  }

  private fun handlePong(json: JSONObject) {
    val sentTs = json.optLong("ts", 0L)
    if (sentTs > 0) {
      val rtt = max(1L, System.currentTimeMillis() - sentTs)
      _latencyMs.value = rtt
      emitTelemetryEvent(TelemetryStreamEvent.LatencyMeasured(rtt))
    }
  }

  // --- RF SIGNAL PIPELINE ---
  private fun handleRfFrame(json: JSONObject) {
    val data = json.optJSONObject("data") ?: json
    val ts = data.optLong("ts", System.currentTimeMillis())
    val peakBand = data.optString("peak_band", "2.4 GHz ISM")
    val peakPower = data.optDouble("peak_power_dbm", -95.0)

    val bandsList = mutableListOf<RFBandInfo>()
    val bandsArr = data.optJSONArray("bands")
    if (bandsArr != null) {
      for (i in 0 until bandsArr.length()) {
        val b = bandsArr.optJSONObject(i) ?: continue
        val name = b.optString("name", "RF Band $i")
        val group = b.optString("group", "RF")
        val center = b.optDouble("center_mhz", 2400.0)
        val proto = b.optString("protocol", "Standard")
        val mod = b.optString("modulation", "Auto")
        val power = b.optDouble("power_dbm", -95.0)
        val alert = b.optBoolean("alert", false)
        val threat = when (b.optString("threat_level", "NOMINAL").uppercase()) {
          "CRITICAL" -> ThreatLevel.CRITICAL
          "ELEVATED", "HIGH", "WARNING" -> ThreatLevel.ELEVATED
          else -> ThreatLevel.NOMINAL
        }
        bandsList.add(RFBandInfo(name, group, center, proto, mod, powerDbm = power, alert = alert, threatLevel = threat))
      }
    }

    val spectraMap = mutableMapOf<String, SpectrumData>()
    val spectraObj = data.optJSONObject("spectra")
    if (spectraObj != null) {
      val keys = spectraObj.keys()
      while (keys.hasNext()) {
        val k = keys.next()
        val spec = spectraObj.optJSONObject(k) ?: continue
        val psdArr = spec.optJSONArray("psd")
        val freqArr = spec.optJSONArray("freq_mhz")
        val label = spec.optString("label", k)
        val center = spec.optDouble("center_mhz", 0.0)
        val hw = spec.optString("hw_note", "")

        val psd = jsonArrayToDoubleList(psdArr)
        val freq = jsonArrayToDoubleList(freqArr)
        spectraMap[k] = SpectrumData(psd, freq, label, center, hw)
      }
    }

    val frame = RFSpectrumFrame(
      timestamp = ts,
      bands = bandsList,
      spectra = spectraMap,
      peakBand = peakBand,
      peakPowerDbm = peakPower
    )
    _rfFrame.value = frame
    emitTelemetryEvent(TelemetryStreamEvent.RfFrameReceived(frame))
  }

  // --- FSO SIGNAL PIPELINE ---
  private fun handleFsoFrame(json: JSONObject) {
    val data = json.optJSONObject("data") ?: json
    val ts = data.optLong("ts", System.currentTimeMillis())
    val attenuation = data.optDouble("overall_attenuation_db", 3.0)
    val allBlocked = data.optBoolean("all_blocked", false)
    val lux = data.optDouble("ambient_lux", 300.0)

    val bandsList = mutableListOf<FSOBandInfo>()
    val bandsArr = data.optJSONArray("bands")
    if (bandsArr != null) {
      for (i in 0 until bandsArr.length()) {
        val b = bandsArr.optJSONObject(i) ?: continue
        val name = b.optString("name", "FSO Band $i")
        val freqThz = b.optDouble("freq_thz", 193.4)
        val wl = b.optDouble("wavelength_nm", 1550.0)
        val mod = b.optString("modulation", "OOK")
        val power = b.optDouble("power_dbm", -65.0)
        val alert = b.optBoolean("alert", false)
        val blocked = b.optBoolean("blocked", false)
        val shutter = when (b.optString("shutter_state", "OPEN").uppercase()) {
          "BLOCKED", "CLOSED" -> ShutterState.BLOCKED
          "SHIELDED" -> ShutterState.SHIELDED
          else -> ShutterState.OPEN
        }
        val bitrate = b.optDouble("bitrate_gbps", 1.0)
        val snr = b.optDouble("snr_db", 25.0)
        val threat = when (b.optString("threat_level", "NOMINAL").uppercase()) {
          "CRITICAL" -> ThreatLevel.CRITICAL
          "ELEVATED", "WARNING", "HIGH" -> ThreatLevel.ELEVATED
          else -> ThreatLevel.NOMINAL
        }

        bandsList.add(
          FSOBandInfo(
            name = name,
            freqThz = freqThz,
            wavelengthNm = wl,
            modulation = mod,
            powerDbm = power,
            alert = alert,
            blocked = blocked,
            shutterState = shutter,
            bitrateGbps = bitrate,
            snrDb = snr,
            threatLevel = threat
          )
        )
      }
    }

    val psd = jsonArrayToDoubleList(data.optJSONArray("psd"))
    val wls = jsonArrayToDoubleList(data.optJSONArray("wavelengths_nm"))

    val frame = FSOSpectrumFrame(
      timestamp = ts,
      bands = bandsList,
      psd = psd,
      wavelengthsNm = wls,
      overallAttenuationDb = attenuation,
      allBlocked = allBlocked,
      ambientLux = lux
    )
    _fsoFrame.value = frame
    emitTelemetryEvent(TelemetryStreamEvent.FsoFrameReceived(frame))
  }

  // --- DETECTION EVENTS PIPELINE ---
  private fun handleDetectionEvent(json: JSONObject) {
    val srcStr = json.optString("source", "rf").uppercase()
    val source = if (srcStr == "FSO") SignalSource.FSO else SignalSource.RF
    val data = json.optJSONObject("data") ?: json

    val event = DetectionEvent(
      id = data.optString("id", "EVT-${System.currentTimeMillis() % 100000}"),
      timestamp = data.optLong("timestamp", System.currentTimeMillis()),
      source = source,
      band = data.optString("band", "Unknown Band"),
      powerDbm = data.optDouble("power_dbm", -50.0),
      thresholdDbm = data.optDouble("threshold_dbm", -60.0),
      threatType = data.optString("threat_type", "ANOMALY"),
      severity = when (data.optString("severity", "MEDIUM").uppercase()) {
        "CRITICAL" -> EventSeverity.CRITICAL
        "HIGH" -> EventSeverity.HIGH
        "LOW", "INFO" -> EventSeverity.INFO
        else -> EventSeverity.MEDIUM
      },
      details = data.optString("details", "Sensor triggered anomaly."),
      actionTaken = data.optString("action_taken", "RECORDED")
    )

    scope.launch {
      _detectionEvents.emit(event)
    }
    emitTelemetryEvent(TelemetryStreamEvent.DetectionEventReceived(event))
  }

  private fun handleSpatialEmitters(json: JSONObject) {
    val data = json.optJSONObject("data") ?: json
    val arr = data.optJSONArray("emitters") ?: return
    val list = mutableListOf<SpatialEmitter>()
    for (i in 0 until arr.length()) {
      val obj = arr.optJSONObject(i) ?: continue
      val src = if (obj.optString("source", "RF").uppercase() == "FSO") SignalSource.FSO else SignalSource.RF
      list.add(
        SpatialEmitter(
          id = obj.optString("id", "EM-$i"),
          label = obj.optString("label", "Emitter $i"),
          source = src,
          azimuthDeg = obj.optDouble("azimuth_deg", 0.0).toFloat(),
          elevationDeg = obj.optDouble("elevation_deg", 0.0).toFloat(),
          uncertaintyDeg = obj.optDouble("uncertainty_deg", 4.5).toFloat(),
          powerDbm = obj.optDouble("power_dbm", -70.0),
          frequencyOrWavelength = obj.optString("frequency", "2.4 GHz"),
          distanceEstMeters = obj.optDouble("distance_m", 50.0),
          isHostile = obj.optBoolean("is_hostile", false)
        )
      )
    }
    _spatialEmitters.value = list
    emitTelemetryEvent(TelemetryStreamEvent.SpatialEmittersReceived(list))
  }

  private fun handleModulationClassification(json: JSONObject) {
    val data = json.optJSONObject("data") ?: json
    val mod = ModulationClassification(
      modulation = data.optString("modulation", "1024-QAM (802.11ax)"),
      confidence = data.optDouble("confidence", 0.94).toFloat(),
      snrEstDb = data.optDouble("snr_db", 28.5),
      baudRateEstKhz = data.optDouble("baud_rate_khz", 80000.0),
      algorithm = data.optString("algorithm", "RadioML ResNet-1D / CNN (arXiv:1602.04105)"),
      cyclostationaryPeakAlpha = data.optDouble("cyclostationary_alpha", 40.0)
    )
    _modulationClassification.value = mod
    emitTelemetryEvent(TelemetryStreamEvent.ModulationClassified(mod))
  }

  private fun handleDecodedFrames(json: JSONObject) {
    val data = json.optJSONObject("data") ?: json
    val arr = data.optJSONArray("frames") ?: return
    val list = mutableListOf<DecodedFrame>()
    for (i in 0 until arr.length()) {
      val obj = arr.optJSONObject(i) ?: continue
      val protoStr = obj.optString("protocol", "WIFI").uppercase()
      val proto = when {
        protoStr.contains("WIFI") || protoStr.contains("802.11") -> FrameProtocolType.WIFI_802_11
        protoStr.contains("BLE") || protoStr.contains("BLUETOOTH") -> FrameProtocolType.BLE_ADV
        protoStr.contains("LTE") || protoStr.contains("CELL") -> FrameProtocolType.CELLULAR_LTE_SIB
        protoStr.contains("LORA") -> FrameProtocolType.LORA_CSS
        protoStr.contains("OPTICAL") || protoStr.contains("FSO") || protoStr.contains("PPM") -> FrameProtocolType.OPTICAL_PPM_FSO
        else -> FrameProtocolType.WIFI_802_11
      }

      list.add(
        DecodedFrame(
          id = obj.optString("id", "FR-$i"),
          timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
          protocol = proto,
          channelOrFreq = obj.optString("channel", obj.optString("channel_or_freq", "Ch 6 (2437 MHz)")),
          sourceAddress = obj.optString("src", obj.optString("source_address", "F4:F5:DB:12:34:56")),
          destinationAddress = obj.optString("dst", obj.optString("destination_address", "FF:FF:FF:FF:FF:FF")),
          rssiDbm = obj.optDouble("rssi_dbm", obj.optDouble("power_dbm", -65.0)),
          snrDb = obj.optDouble("snr_db", 22.0),
          summary = obj.optString("summary", "Decoded Frame"),
          payloadHex = obj.optString("payload", obj.optString("payload_hex", "08 00 00 00 FF FF"))
        )
      )
    }
    _decodedFrames.value = list
    emitTelemetryEvent(TelemetryStreamEvent.DecodedFramesReceived(list))
  }

  private fun handleServerStatus(json: JSONObject) {
    var isScanning = _isServerScanning.value
    var fsoBlocked = _fsoAllBlocked.value

    if (json.has("scanning")) {
      isScanning = json.optBoolean("scanning", true)
      _isServerScanning.value = isScanning
    }
    if (json.has("fso_all_blocked")) {
      fsoBlocked = json.optBoolean("fso_all_blocked", false)
      _fsoAllBlocked.value = fsoBlocked
    }
    emitTelemetryEvent(TelemetryStreamEvent.ServerStatusUpdated(isScanning, fsoBlocked))
  }

  // --- BIDIRECTIONAL CONTROL COMMANDS ---
  fun sendRfThresholds(alertDbm: Float, highDbm: Float) {
    sendJson(
      JSONObject()
        .put("cmd", "set_rf_thresholds")
        .put("alert_dbm", alertDbm.toDouble())
        .put("high_dbm", highDbm.toDouble())
    )
  }

  fun sendFsoThresholds(alertDbm: Float, blockDbm: Float) {
    sendJson(
      JSONObject()
        .put("cmd", "set_fso_thresholds")
        .put("alert_dbm", alertDbm.toDouble())
        .put("block_dbm", blockDbm.toDouble())
    )
  }

  fun sendFsoBlock(band: String) {
    sendJson(JSONObject().put("cmd", "fso_block").put("band", band))
  }

  fun sendFsoUnblock(band: String) {
    sendJson(JSONObject().put("cmd", "fso_unblock").put("band", band))
  }

  fun sendFsoBlockAll() {
    sendJson(JSONObject().put("cmd", "fso_block_all"))
    _fsoAllBlocked.value = true
  }

  fun sendFsoReset() {
    sendJson(JSONObject().put("cmd", "fso_reset"))
    _fsoAllBlocked.value = false
  }

  fun sendStartScan() {
    sendJson(JSONObject().put("cmd", "start_scan"))
    _isServerScanning.value = true
  }

  fun sendStopScan() {
    sendJson(JSONObject().put("cmd", "stop_scan"))
    _isServerScanning.value = false
  }

  fun sendPing() {
    val now = System.currentTimeMillis()
    sendJson(JSONObject().put("cmd", "ping").put("ts", now))
  }

  fun sendFetchRfPacket() {
    val now = System.currentTimeMillis()
    sendJson(
      JSONObject()
        .put("cmd", "fetch_rf_packet")
        .put("pipeline", "RF")
        .put("ts", now)
    )
  }

  fun sendFetchFsoPacket() {
    val now = System.currentTimeMillis()
    sendJson(
      JSONObject()
        .put("cmd", "fetch_fso_packet")
        .put("pipeline", "FSO")
        .put("ts", now)
    )
  }

  fun sendPollTelemetry(pipelines: List<String> = listOf("RF", "FSO")) {
    val now = System.currentTimeMillis()
    sendJson(
      JSONObject()
        .put("cmd", "poll_telemetry")
        .put("pipelines", JSONArray(pipelines))
        .put("ts", now)
    )
  }

  fun sendExportRequest() {
    sendJson(JSONObject().put("cmd", "export_request"))
  }

  fun sendJson(json: JSONObject) {
    webSocket?.send(json.toString())
  }

  fun sendRaw(text: String) {
    webSocket?.send(text)
  }

  private fun emitTelemetryEvent(event: TelemetryStreamEvent) {
    scope.launch {
      _telemetryEvents.emit(event)
    }
  }

  private fun jsonArrayToDoubleList(arr: JSONArray?): List<Double> {
    if (arr == null) return emptyList()
    val list = mutableListOf<Double>()
    for (i in 0 until arr.length()) {
      list.add(arr.optDouble(i, 0.0))
    }
    return list
  }

  private fun Double.format(digits: Int) = "%.${digits}f".format(this)
}
