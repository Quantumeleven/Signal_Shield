package com.example.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.SignalShieldDatabase
import com.example.data.ThreatLogRepository
import com.example.engine.SignalSimulationEngine
import com.example.model.ConnectionStatus
import com.example.model.FSOSpectrumFrame
import com.example.model.RFSpectrumFrame
import com.example.model.ShieldConfig
import com.example.network.SignalBridgeClient
import com.example.network.TelemetryStreamEvent
import com.example.network.WebSocketManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Background Service that maintains a persistent WebSocket connection and periodically
 * fetches incoming signal packets for both the RF and FSO telemetry pipelines.
 */
class SignalTelemetryService : Service() {

  companion object {
    private const val TAG = "SignalTelemetryService"
    const val NOTIFICATION_ID = 4040
    const val CHANNEL_ID = "signal_telemetry_pipeline_channel"

    // Intent Actions
    const val ACTION_START = "com.example.service.ACTION_START"
    const val ACTION_STOP = "com.example.service.ACTION_STOP"
    const val ACTION_FETCH_NOW = "com.example.service.ACTION_FETCH_NOW"
    const val ACTION_UPDATE_INTERVAL = "com.example.service.ACTION_UPDATE_INTERVAL"
    const val ACTION_CONNECT_URL = "com.example.service.ACTION_CONNECT_URL"

    // Intent Extras
    const val EXTRA_URL = "extra_ws_url"
    const val EXTRA_PORT = "extra_ws_port"
    const val EXTRA_INTERVAL_MS = "extra_interval_ms"

    // Observable Static Telemetry State for ViewModels and UI
    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    private val _rfPacketsFetched = MutableStateFlow(0L)
    val rfPacketsFetched: StateFlow<Long> = _rfPacketsFetched.asStateFlow()

    private val _fsoPacketsFetched = MutableStateFlow(0L)
    val fsoPacketsFetched: StateFlow<Long> = _fsoPacketsFetched.asStateFlow()

    private val _lastFetchTimestamp = MutableStateFlow(0L)
    val lastFetchTimestamp: StateFlow<Long> = _lastFetchTimestamp.asStateFlow()

    private val _fetchIntervalMs = MutableStateFlow(1000L)
    val fetchIntervalMs: StateFlow<Long> = _fetchIntervalMs.asStateFlow()

    private val _serviceConnectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val serviceConnectionStatus: StateFlow<ConnectionStatus> = _serviceConnectionStatus.asStateFlow()

    private val _serviceLog = MutableStateFlow("Background WebSocket service idle")
    val serviceLog: StateFlow<String> = _serviceLog.asStateFlow()

    private val _latestRfPacket = MutableStateFlow<RFSpectrumFrame?>(null)
    val latestRfPacket: StateFlow<RFSpectrumFrame?> = _latestRfPacket.asStateFlow()

    private val _latestFsoPacket = MutableStateFlow<FSOSpectrumFrame?>(null)
    val latestFsoPacket: StateFlow<FSOSpectrumFrame?> = _latestFsoPacket.asStateFlow()

    /**
     * Start or command the background telemetry fetch service.
     */
    fun start(context: Context, url: String? = null, port: Int = 8765, intervalMs: Long = 1000L) {
      val intent = Intent(context, SignalTelemetryService::class.java).apply {
        action = ACTION_START
        url?.let { putExtra(EXTRA_URL, it) }
        putExtra(EXTRA_PORT, port)
        putExtra(EXTRA_INTERVAL_MS, intervalMs)
      }
      try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          context.startForegroundService(intent)
        } else {
          context.startService(intent)
        }
      } catch (e: Exception) {
        Log.w(TAG, "startForegroundService failed, starting normal service: ${e.message}")
        try {
          context.startService(intent)
        } catch (inner: Exception) {
          Log.e(TAG, "Failed to start service: ${inner.message}")
        }
      }
    }

    /**
     * Stop the background telemetry fetch service.
     */
    fun stop(context: Context) {
      val intent = Intent(context, SignalTelemetryService::class.java).apply {
        action = ACTION_STOP
      }
      try {
        context.startService(intent)
      } catch (e: Exception) {
        Log.e(TAG, "Failed to stop service: ${e.message}")
      }
    }

    /**
     * Force an immediate out-of-band packet fetch for RF & FSO pipelines.
     */
    fun fetchNow(context: Context) {
      val intent = Intent(context, SignalTelemetryService::class.java).apply {
        action = ACTION_FETCH_NOW
      }
      try {
        context.startService(intent)
      } catch (e: Exception) {
        Log.e(TAG, "Failed to send fetchNow intent: ${e.message}")
      }
    }

    /**
     * Adjust periodic packet fetch cadence in milliseconds.
     */
    fun updateInterval(context: Context, intervalMs: Long) {
      val intent = Intent(context, SignalTelemetryService::class.java).apply {
        action = ACTION_UPDATE_INTERVAL
        putExtra(EXTRA_INTERVAL_MS, intervalMs)
      }
      try {
        context.startService(intent)
      } catch (e: Exception) {
        Log.e(TAG, "Failed to update interval: ${e.message}")
      }
    }

    /**
     * Connect or switch the target WebSocket bridge endpoint in background.
     */
    fun connectUrl(context: Context, url: String, port: Int = 8765) {
      val intent = Intent(context, SignalTelemetryService::class.java).apply {
        action = ACTION_CONNECT_URL
        putExtra(EXTRA_URL, url)
        putExtra(EXTRA_PORT, port)
      }
      try {
        context.startService(intent)
      } catch (e: Exception) {
        Log.e(TAG, "Failed to send connectUrl intent: ${e.message}")
      }
    }
  }

  private val serviceJob = SupervisorJob()
  private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

  private lateinit var bridgeClient: SignalBridgeClient
  private val simulationEngine = SignalSimulationEngine()
  private var threatLogRepository: ThreatLogRepository? = null

  private var fetchJob: Job? = null
  private val binder = LocalBinder()

  inner class LocalBinder : Binder() {
    fun getService(): SignalTelemetryService = this@SignalTelemetryService
  }

  override fun onBind(intent: Intent?): IBinder = binder

  override fun onCreate() {
    super.onCreate()
    Log.i(TAG, "SignalTelemetryService created")
    createNotificationChannel()

    // Initialize Room Database repository for background event logging
    try {
      val db = SignalShieldDatabase.getDatabase(applicationContext)
      threatLogRepository = ThreatLogRepository(
        threatLogDao = db.threatLogDao(),
        systemEventDao = db.systemEventDao(),
        bioMetricDataDao = db.bioMetricDataDao()
      )
    } catch (e: Exception) {
      Log.e(TAG, "Database init warning: ${e.message}")
    }

    // Initialize WebSocket Bridge Client
    bridgeClient = SignalBridgeClient(serviceScope, applicationContext)

    // Observe connection state
    serviceScope.launch {
      bridgeClient.connectionStatus.collect { status ->
        _serviceConnectionStatus.value = status
        updateNotification()
      }
    }

    // Observe incoming RF packets
    serviceScope.launch {
      bridgeClient.rfFrame.collect { frame ->
        if (frame.bands.isNotEmpty()) {
          _latestRfPacket.value = frame
          _rfPacketsFetched.value++
          _lastFetchTimestamp.value = System.currentTimeMillis()
          _serviceLog.value = "Rx RF packet: ${frame.peakBand} (${"%.1f".format(frame.peakPowerDbm)} dBm)"
        }
      }
    }

    // Observe incoming FSO packets
    serviceScope.launch {
      bridgeClient.fsoFrame.collect { frame ->
        if (frame.bands.isNotEmpty()) {
          _latestFsoPacket.value = frame
          _fsoPacketsFetched.value++
          _lastFetchTimestamp.value = System.currentTimeMillis()
          _serviceLog.value = "Rx FSO packet: ${frame.bands.size} optical bands (attn ${"%.1f".format(frame.overallAttenuationDb)} dB)"
        }
      }
    }

    // Observe telemetry events
    serviceScope.launch {
      bridgeClient.telemetryEvents.collect { event ->
        when (event) {
          is TelemetryStreamEvent.PacketFetchCompleted -> {
            _lastFetchTimestamp.value = event.timestamp
          }
          is TelemetryStreamEvent.ConnectionStateChanged -> {
            _serviceLog.value = "WebSocket: ${event.newStatus} (${event.message})"
          }
          is TelemetryStreamEvent.ErrorOccurred -> {
            _serviceLog.value = "WebSocket error: ${event.error}"
          }
          else -> Unit
        }
      }
    }
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_STOP -> {
        Log.i(TAG, "Stopping SignalTelemetryService via ACTION_STOP")
        stopPeriodicFetchLoop()
        bridgeClient.disconnect()
        _isServiceRunning.value = false
        _serviceLog.value = "Service stopped"
        stopForegroundCompat()
        stopSelf()
        return START_NOT_STICKY
      }

      ACTION_FETCH_NOW -> {
        Log.i(TAG, "Triggering immediate manual packet fetch via ACTION_FETCH_NOW")
        serviceScope.launch {
          performPacketFetch()
        }
      }

      ACTION_UPDATE_INTERVAL -> {
        val newInterval = intent.getLongExtra(EXTRA_INTERVAL_MS, 1000L).coerceIn(250L, 10000L)
        _fetchIntervalMs.value = newInterval
        Log.i(TAG, "Updated packet fetch interval to ${newInterval}ms")
        startPeriodicFetchLoop()
      }

      ACTION_CONNECT_URL -> {
        val targetUrl = intent.getStringExtra(EXTRA_URL)
        val targetPort = intent.getIntExtra(EXTRA_PORT, 8765)
        if (!targetUrl.isNullOrBlank()) {
          bridgeClient.connect(targetUrl, targetPort, "Background WebSocket Sink")
        }
      }

      ACTION_START, null -> {
        val targetUrl = intent?.getStringExtra(EXTRA_URL)
        val targetPort = intent?.getIntExtra(EXTRA_PORT, 8765) ?: 8765
        val intervalMs = intent?.getLongExtra(EXTRA_INTERVAL_MS, _fetchIntervalMs.value) ?: _fetchIntervalMs.value
        _fetchIntervalMs.value = intervalMs.coerceIn(250L, 10000L)

        startForegroundCompat()

        if (!targetUrl.isNullOrBlank()) {
          bridgeClient.connect(targetUrl, targetPort, "Background Telemetry Bridge")
        } else if (_serviceConnectionStatus.value == ConnectionStatus.DISCONNECTED) {
          // Connect to default URL (adapt to emulator vs physical phone)
          val defaultUrl = WebSocketManager.getDefaultBaseUrl(applicationContext, targetPort)
          bridgeClient.connect(defaultUrl, targetPort, "Auto-Discovered Background Sink")
        }

        startPeriodicFetchLoop()
      }
    }

    return START_STICKY
  }

  private fun startPeriodicFetchLoop() {
    fetchJob?.cancel()
    fetchJob = serviceScope.launch(Dispatchers.IO) {
      _isServiceRunning.value = true
      _serviceLog.value = "Periodic fetch loop active (interval: ${_fetchIntervalMs.value}ms)"
      Log.i(TAG, "Starting periodic packet fetch loop every ${_fetchIntervalMs.value}ms")

      var tickCounter = 0
      while (isActive) {
        performPacketFetch()
        tickCounter++
        if (tickCounter % 5 == 0) {
          withContext(Dispatchers.Main) {
            updateNotification()
          }
        }
        delay(_fetchIntervalMs.value)
      }
    }
  }

  private fun stopPeriodicFetchLoop() {
    fetchJob?.cancel()
    fetchJob = null
    _isServiceRunning.value = false
  }

  /**
   * Fetches incoming signal packets for both the RF and FSO pipelines.
   * If WebSocket is connected, sends explicit query/poll commands over the socket.
   * If disconnected or in simulation fallback, generates synthetic packets to ensure
   * uninterrupted pipeline processing.
   */
  private suspend fun performPacketFetch() {
    val isConnected = bridgeClient.connectionStatus.value == ConnectionStatus.CONNECTED

    if (isConnected) {
      // Periodic WebSocket fetch requests
      bridgeClient.sendPollTelemetry(listOf("RF", "FSO"))
      bridgeClient.sendFetchRfPacket()
      bridgeClient.sendFetchFsoPacket()
      _lastFetchTimestamp.value = System.currentTimeMillis()
    } else {
      // Simulation / offline fallback generation to feed the RF & FSO pipelines
      val (simulatedRf, rfThreat) = simulationEngine.generateRfFrame(ShieldConfig())
      val (simulatedFso, fsoThreat) = simulationEngine.generateFsoFrame(ShieldConfig())

      _latestRfPacket.value = simulatedRf
      _latestFsoPacket.value = simulatedFso
      _rfPacketsFetched.value++
      _fsoPacketsFetched.value++
      _lastFetchTimestamp.value = System.currentTimeMillis()

      // Log threat events if detected
      if (rfThreat != null) {
        threatLogRepository?.recordThreat(
          detectionEvent = rfThreat,
          notes = "Detected by Background WebSocket Service Pipeline"
        )
      }
      if (fsoThreat != null) {
        threatLogRepository?.recordThreat(
          detectionEvent = fsoThreat,
          notes = "Detected by Background Optical Pipeline"
        )
      }
    }
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_ID,
        "Signal Shield Telemetry Service",
        NotificationManager.IMPORTANCE_LOW
      ).apply {
        description = "Monitors background WebSocket connection and periodically fetches RF/FSO signal packets"
        enableLights(true)
        lightColor = Color.CYAN
        setShowBadge(false)
      }
      val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      manager.createNotificationChannel(channel)
    }
  }

  private fun buildNotification(): Notification {
    val pendingIntent = PendingIntent.getActivity(
      this,
      0,
      Intent(this, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
      },
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val fetchNowIntent = PendingIntent.getService(
      this,
      1,
      Intent(this, SignalTelemetryService::class.java).apply { action = ACTION_FETCH_NOW },
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val stopIntent = PendingIntent.getService(
      this,
      2,
      Intent(this, SignalTelemetryService::class.java).apply { action = ACTION_STOP },
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val statusText = when (_serviceConnectionStatus.value) {
      ConnectionStatus.CONNECTED -> "WebSocket CONNECTED"
      ConnectionStatus.CONNECTING -> "WebSocket CONNECTING..."
      ConnectionStatus.SIMULATION -> "SIMULATION PIPELINE"
      ConnectionStatus.DISCONNECTED -> "STANDBY"
    }

    val contentText = "RF: ${_rfPacketsFetched.value} pkts | FSO: ${_fsoPacketsFetched.value} pkts • $statusText"

    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setContentTitle("Signal Shield Background Telemetry")
      .setContentText(contentText)
      .setSmallIcon(R.drawable.ic_signal_telemetry)
      .setContentIntent(pendingIntent)
      .setOngoing(true)
      .setOnlyAlertOnce(true)
      .addAction(android.R.drawable.ic_popup_sync, "Fetch Now", fetchNowIntent)
      .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopIntent)
      .build()
  }

  private fun startForegroundCompat() {
    val notification = buildNotification()
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        startForeground(
          NOTIFICATION_ID,
          notification,
          ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
      } else {
        startForeground(NOTIFICATION_ID, notification)
      }
    } catch (e: Exception) {
      Log.w(TAG, "startForeground failed (permission or restriction): ${e.message}")
    }
  }

  private fun updateNotification() {
    if (!_isServiceRunning.value) return
    try {
      val notification = buildNotification()
      val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      manager.notify(NOTIFICATION_ID, notification)
    } catch (e: Exception) {
      Log.w(TAG, "updateNotification failed: ${e.message}")
    }
  }

  private fun stopForegroundCompat() {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        stopForeground(STOP_FOREGROUND_REMOVE)
      } else {
        @Suppress("DEPRECATION")
        stopForeground(true)
      }
    } catch (e: Exception) {
      Log.w(TAG, "stopForeground error: ${e.message}")
    }
  }

  override fun onDestroy() {
    Log.i(TAG, "SignalTelemetryService destroyed")
    stopPeriodicFetchLoop()
    bridgeClient.disconnect()
    serviceJob.cancel()
    _isServiceRunning.value = false
    _serviceLog.value = "Service destroyed"
    stopForegroundCompat()
    super.onDestroy()
  }
}
