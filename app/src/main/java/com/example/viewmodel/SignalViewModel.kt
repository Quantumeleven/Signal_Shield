package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SignalShieldDatabase
import com.example.data.ThreatLogRepository
import com.example.data.entity.BioMetricData
import com.example.data.entity.SystemEventEntity
import com.example.data.entity.ThreatLogEntity
import com.example.engine.BiomedicalTelemetryEngine
import com.example.engine.SignalSimulationEngine
import com.example.model.*
import com.example.network.GeminiThreatClassifier
import com.example.network.SignalBridgeClient
import com.example.network.TelemetryStreamEvent
import com.example.service.SignalTelemetryService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class SignalViewModel(application: Application) : AndroidViewModel(application) {
  private val bridgeClient = SignalBridgeClient(viewModelScope, application)
  private val simulationEngine = SignalSimulationEngine()
  private val biomedicalEngine = BiomedicalTelemetryEngine()
  private val geminiThreatClassifier = GeminiThreatClassifier()

  // Room Database & Repository for Persistent Signal Threat, System Event, & BioMetric Telemetry Logging
  private val database = SignalShieldDatabase.getDatabase(application)
  val threatLogRepository = ThreatLogRepository(
    threatLogDao = database.threatLogDao(),
    systemEventDao = database.systemEventDao(),
    bioMetricDataDao = database.bioMetricDataDao()
  )

  // State flows from WebSocket Manager / Bridge
  val connectionStatus: StateFlow<ConnectionStatus> = bridgeClient.connectionStatus
  val latencyMs: StateFlow<Long> = bridgeClient.latencyMs

  private val _isScanning = MutableStateFlow(true)
  val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

  // Bio-Medical Telemetry & Patient Device Fleet State
  private val _biomedicalDevices = MutableStateFlow<List<BiomedicalDevice>>(emptyList())
  val biomedicalDevices: StateFlow<List<BiomedicalDevice>> = _biomedicalDevices.asStateFlow()

  private val _selectedBiomedicalDevice = MutableStateFlow<BiomedicalDevice?>(null)
  val selectedBiomedicalDevice: StateFlow<BiomedicalDevice?> = _selectedBiomedicalDevice.asStateFlow()

  private val _shieldConfig = MutableStateFlow(ShieldConfig())
  val shieldConfig: StateFlow<ShieldConfig> = _shieldConfig.asStateFlow()

  private val _rfFrame = MutableStateFlow(RFSpectrumFrame())
  val rfFrame: StateFlow<RFSpectrumFrame> = _rfFrame.asStateFlow()

  private val _fsoFrame = MutableStateFlow(FSOSpectrumFrame())
  val fsoFrame: StateFlow<FSOSpectrumFrame> = _fsoFrame.asStateFlow()

  private val _events = MutableStateFlow<List<DetectionEvent>>(emptyList())
  val events: StateFlow<List<DetectionEvent>> = _events.asStateFlow()

  private val _waterfallHistory = MutableStateFlow<List<WaterfallRow>>(emptyList())
  val waterfallHistory: StateFlow<List<WaterfallRow>> = _waterfallHistory.asStateFlow()

  // Active selected RF group in spectrum analyzer
  private val _selectedRfGroup = MutableStateFlow("2.4GHz")
  val selectedRfGroup: StateFlow<String> = _selectedRfGroup.asStateFlow()

  // Device networking & emulator detection
  val isRunningOnEmulator: Boolean = com.example.util.NetworkUtils.isRunningOnEmulator()

  private val _deviceLocalIp = MutableStateFlow<String?>(com.example.util.NetworkUtils.getDeviceLocalIpAddress(application))
  val deviceLocalIp: StateFlow<String?> = _deviceLocalIp.asStateFlow()

  private val _subnetPrefix = MutableStateFlow(com.example.util.NetworkUtils.getSubnetPrefix(_deviceLocalIp.value))
  val subnetPrefix: StateFlow<String> = _subnetPrefix.asStateFlow()

  private val _probeStatusMessage = MutableStateFlow("")
  val probeStatusMessage: StateFlow<String> = _probeStatusMessage.asStateFlow()

  private val _isProbing = MutableStateFlow(false)
  val isProbing: StateFlow<Boolean> = _isProbing.asStateFlow()

  // GNU Radio & SDR Bridge connection settings (Default adapts to physical phone vs emulator)
  private val _bridgeHost = MutableStateFlow(
    if (isRunningOnEmulator) "ws://10.0.2.2:8765" else "ws://127.0.0.1:8765"
  )
  val bridgeHost: StateFlow<String> = _bridgeHost.asStateFlow()

  private val _bridgePort = MutableStateFlow("8765")
  val bridgePort: StateFlow<String> = _bridgePort.asStateFlow()

  val activeServerUrl: StateFlow<String> = bridgeClient.activeServerUrl
  val serverDescription: StateFlow<String> = bridgeClient.serverDescription
  val lastLogMessage: StateFlow<String> = bridgeClient.lastLogMessage

  // Official GNU Radio & SDR Presets tailored for both Physical Phones & Emulator
  val serverPresets: List<ServerPreset> = if (!isRunningOnEmulator) {
    val prefix = _subnetPrefix.value
    listOf(
      ServerPreset(
        id = "adb_reverse_bridge",
        title = "USB Cable (ADB Reverse 127.0.0.1)",
        institution = "Android Debug Bridge (Fast & Direct)",
        url = "ws://127.0.0.1:8765",
        defaultPort = 8765,
        description = "Direct USB pipe. Run 'adb reverse tcp:8765 tcp:8765' in terminal on your computer.",
        isSecure = false,
        tag = "USB / ADB"
      ),
      ServerPreset(
        id = "gnuradio_lan_sink",
        title = "Wi-Fi LAN Host (${prefix}X)",
        institution = "Local Wireless Network",
        url = "ws://${prefix}100:8765",
        defaultPort = 8765,
        description = "Wireless LAN connection to PC. Replace with your PC's IP (e.g. ${prefix}105).",
        isSecure = false,
        tag = "WI-FI LAN"
      ),
      ServerPreset(
        id = "gnuradio_grc_bridge",
        title = "GNU Radio Companion Spectrum Bridge",
        institution = "GRC Fast Fourier Engine",
        url = "ws://127.0.0.1:9001",
        defaultPort = 9001,
        description = "Wideband RF PSD stream (run 'adb reverse tcp:9001 tcp:9001' or use PC Wi-Fi IP).",
        isSecure = false,
        tag = "GRC"
      ),
      ServerPreset(
        id = "gnuradio_fso_optical",
        title = "GNU Radio FSO / Optical Stream",
        institution = "Thorlabs QPD / Laser Rx Block",
        url = "ws://127.0.0.1:8766",
        defaultPort = 8766,
        description = "Free-space optical detector & avalanche photodiode stream with shutter telemetry.",
        isSecure = false,
        tag = "GNU RADIO"
      ),
      ServerPreset(
        id = "gnuradio_emulator_fallback",
        title = "Android Studio Emulator (10.0.2.2)",
        institution = "AVD Virtual Router (Emulator only)",
        url = "ws://10.0.2.2:8765",
        defaultPort = 8765,
        description = "Loopback router for Android Studio Virtual Devices (AVD). Does not work on physical phones.",
        isSecure = false,
        tag = "EMULATOR"
      ),
      ServerPreset(
        id = "echo_public_test",
        title = "Public SDR & WebSocket Echo Relay",
        institution = "Open WebSocket Network",
        url = "wss://echo.websocket.events",
        defaultPort = 443,
        description = "Public secure WebSocket test server for validating external bidirectional network handshake.",
        isSecure = true,
        tag = "PUBLIC"
      )
    )
  } else {
    listOf(
      ServerPreset(
        id = "gnuradio_flowgraph_sink",
        title = "GNU Radio Live Flowgraph Sink",
        institution = "GNU Radio Core / gr-telemetry",
        url = "ws://10.0.2.2:8765",
        defaultPort = 8765,
        description = "Real-time FFT spectrum & I/Q telemetry pipe from active GNU Radio Companion flowgraph sink.",
        isSecure = false,
        tag = "GNU RADIO"
      ),
      ServerPreset(
        id = "gnuradio_grc_bridge",
        title = "GNU Radio Companion Spectrum Bridge",
        institution = "GRC Fast Fourier Engine",
        url = "ws://10.0.2.2:9001",
        defaultPort = 9001,
        description = "Wideband RF power spectral density (PSD) & waterfall stream from GRC Python block.",
        isSecure = false,
        tag = "GRC"
      ),
      ServerPreset(
        id = "gnuradio_fso_optical",
        title = "GNU Radio FSO / Optical Stream",
        institution = "Thorlabs QPD / Laser Rx Block",
        url = "ws://10.0.2.2:8766",
        defaultPort = 8766,
        description = "Free-space optical detector & avalanche photodiode stream with shutter telemetry.",
        isSecure = false,
        tag = "GNU RADIO"
      ),
      ServerPreset(
        id = "rtlsdr_hackrf_local",
        title = "RTL-SDR / HackRF Local GNU Radio Server",
        institution = "Osmocom / gr-osmosdr",
        url = "ws://10.0.2.2:8080",
        defaultPort = 8080,
        description = "Local hardware SDR transceiver gateway running via GNU Radio Osmocom driver on host machine.",
        isSecure = false,
        tag = "RTL-SDR"
      ),
      ServerPreset(
        id = "echo_public_test",
        title = "Public SDR & WebSocket Echo Relay",
        institution = "Open WebSocket Network",
        url = "wss://echo.websocket.events",
        defaultPort = 443,
        description = "Public secure WebSocket test server for validating external bidirectional network handshake.",
        isSecure = true,
        tag = "PUBLIC"
      )
    )
  }

  private val _selectedPreset = MutableStateFlow<ServerPreset?>(serverPresets[0])
  val selectedPreset: StateFlow<ServerPreset?> = _selectedPreset.asStateFlow()

  // Threat banner state
  private val _activeThreatBanner = MutableStateFlow<DetectionEvent?>(null)
  val activeThreatBanner: StateFlow<DetectionEvent?> = _activeThreatBanner.asStateFlow()

  // Direction Finding & Spatial Emitters State
  private val _spatialEmitters = MutableStateFlow<List<SpatialEmitter>>(emptyList())
  val spatialEmitters: StateFlow<List<SpatialEmitter>> = _spatialEmitters.asStateFlow()

  private val _selectedEmitter = MutableStateFlow<SpatialEmitter?>(null)
  val selectedEmitter: StateFlow<SpatialEmitter?> = _selectedEmitter.asStateFlow()

  private val _opticalTarget = MutableStateFlow(
    OpticalAoATarget(deltaX = 0f, deltaY = 0f, intensityWatts = 0.002, spotSizeMm = 2.0f, wavelengthNm = 1550.0, lockedOn = true)
  )
  val opticalTarget: StateFlow<OpticalAoATarget> = _opticalTarget.asStateFlow()

  // RadioML & Modulation Classification State
  private val _modulationClassification = MutableStateFlow(
    ModulationClassification(
      modulation = "1024-QAM (802.11ax)",
      confidence = 0.94f,
      snrEstDb = 28.5,
      baudRateEstKhz = 80000.0,
      algorithm = "RadioML ResNet-1D / CNN (arXiv:1602.04105)",
      cyclostationaryPeakAlpha = 40.0
    )
  )
  val modulationClassification: StateFlow<ModulationClassification> = _modulationClassification.asStateFlow()

  private val _candidateModulations = MutableStateFlow<List<Pair<String, Float>>>(
    listOf("1024-QAM (802.11ax)" to 0.94f, "64-QAM (802.11n)" to 0.04f, "QPSK (LTE)" to 0.01f, "GFSK (BLE)" to 0.01f)
  )
  val candidateModulations: StateFlow<List<Pair<String, Float>>> = _candidateModulations.asStateFlow()

  // Decoded Protocol Frames Sniffer State
  private val _decodedFrames = MutableStateFlow<List<DecodedFrame>>(emptyList())
  val decodedFrames: StateFlow<List<DecodedFrame>> = _decodedFrames.asStateFlow()

  // SigMF Metadata & JSON
  private val _sigmfMetadata = MutableStateFlow(SigMFMetadata())
  val sigmfMetadata: StateFlow<SigMFMetadata> = _sigmfMetadata.asStateFlow()

  private val _sigmfJson = MutableStateFlow("")
  val sigmfJson: StateFlow<String> = _sigmfJson.asStateFlow()

  // Gemini Stream Anomaly Threat Report State
  private val _streamAnomalyReport = MutableStateFlow(
    StreamAnomalyReport(
      threatCategory = InterferenceCategory.LOW,
      threatCategoryText = "Low",
      interferenceType = "Nominal Background Noise Floor",
      confidenceScore = 0.95f,
      summary = "Live GNU Radio spectrum stream is stable. No hostile jamming or high-energy optical spikes detected.",
      detailedAnalysis = "Evaluated RF 2.4/5.8GHz PSD energy distribution and FSO optical flux levels. Spectral power remains within standard non-interfering limits.",
      mitigationRecommendation = "Maintain passive wideband spectrum monitoring.",
      affectedBand = "All Bands Nominal",
      triggerSource = "GNU Radio Stream Monitor (Gemini 3.5 Flash)"
    )
  )
  val streamAnomalyReport: StateFlow<StreamAnomalyReport> = _streamAnomalyReport.asStateFlow()

  private val _autoScanAnomalies = MutableStateFlow(true)
  val autoScanAnomalies: StateFlow<Boolean> = _autoScanAnomalies.asStateFlow()

  // --- Room Database Persistent Flows ---
  val persistedThreats: StateFlow<List<ThreatLogEntity>> = threatLogRepository.allThreats
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val persistedSystemEvents: StateFlow<List<SystemEventEntity>> = threatLogRepository.allSystemEvents
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val persistedBioMetrics: StateFlow<List<BioMetricData>> = threatLogRepository.allBioMetricData
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val persistentThreatCount: StateFlow<Int> = threatLogRepository.threatCount
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

  val criticalThreatCount: StateFlow<Int> = threatLogRepository.criticalThreatCount
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

  val systemEventCount: StateFlow<Int> = threatLogRepository.systemEventCount
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

  val bioMetricCount: StateFlow<Int> = threatLogRepository.bioMetricCount
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

  // Log Search & Filter State in UI
  private val _logSearchQuery = MutableStateFlow("")
  val logSearchQuery: StateFlow<String> = _logSearchQuery.asStateFlow()

  private val _logFilterType = MutableStateFlow("ALL")
  val logFilterType: StateFlow<String> = _logFilterType.asStateFlow()

  private val _activeLogViewTab = MutableStateFlow(0) // 0 = Threat History, 1 = System Audit Events
  val activeLogViewTab: StateFlow<Int> = _activeLogViewTab.asStateFlow()

  // Background WebSocket Telemetry Service State
  val isBackgroundServiceRunning: StateFlow<Boolean> = SignalTelemetryService.isServiceRunning
  val bgRfPacketsFetched: StateFlow<Long> = SignalTelemetryService.rfPacketsFetched
  val bgFsoPacketsFetched: StateFlow<Long> = SignalTelemetryService.fsoPacketsFetched
  val bgLastFetchTimestamp: StateFlow<Long> = SignalTelemetryService.lastFetchTimestamp
  val bgFetchIntervalMs: StateFlow<Long> = SignalTelemetryService.fetchIntervalMs
  val bgServiceLog: StateFlow<String> = SignalTelemetryService.serviceLog
  val bgServiceConnectionStatus: StateFlow<ConnectionStatus> = SignalTelemetryService.serviceConnectionStatus

  // Settings: FCC Regulation, Location & Emergency Services, HIPAA Authorization
  private val _systemSettings = MutableStateFlow(AppSystemSettings())
  val systemSettings: StateFlow<AppSystemSettings> = _systemSettings.asStateFlow()

  private var lastAnalysisTimestamp = 0L
  private var isAnalysisRunning = false
  private var simLoopJob: Job? = null

  init {
    // Seed initial historical monitoring records in Room DB if empty
    viewModelScope.launch(Dispatchers.IO) {
      threatLogRepository.seedInitialHistoryIfEmpty()
    }

    // Initial setup with simulation engine
    val initRf = simulationEngine.generateRfFrame(_shieldConfig.value).first
    val initFso = simulationEngine.generateFsoFrame(_shieldConfig.value).first
    _rfFrame.value = initRf
    _fsoFrame.value = initFso
    _spatialEmitters.value = simulationEngine.generateSpatialEmitters()
    _opticalTarget.value = simulationEngine.generateOpticalAoATarget()
    val (initClf, initCandidates) = simulationEngine.generateModulationClassification()
    _modulationClassification.value = initClf
    _candidateModulations.value = initCandidates
    _decodedFrames.value = simulationEngine.generateDecodedFrames()
    val (sigMeta, sigJson) = simulationEngine.generateSigMFMetadata()
    _sigmfMetadata.value = sigMeta
    _sigmfJson.value = sigJson

    // Initialize Bio-Medical Device Fleet
    _biomedicalDevices.value = biomedicalEngine.tickDevices()
    _selectedBiomedicalDevice.value = _biomedicalDevices.value.firstOrNull()

    // Listen to network bridge RF frames
    viewModelScope.launch {
      bridgeClient.rfFrame.collect { frame ->
        if (connectionStatus.value == ConnectionStatus.CONNECTED && frame.bands.isNotEmpty()) {
          _rfFrame.value = frame
          updateWaterfall(frame)
        }
      }
    }

    // Listen to network bridge FSO frames
    viewModelScope.launch {
      bridgeClient.fsoFrame.collect { frame ->
        if (connectionStatus.value == ConnectionStatus.CONNECTED && frame.bands.isNotEmpty()) {
          _fsoFrame.value = frame
        }
      }
    }

    // Listen to background service RF frames
    viewModelScope.launch {
      SignalTelemetryService.latestRfPacket.collect { frame ->
        if (frame != null && isBackgroundServiceRunning.value) {
          _rfFrame.value = frame
          updateWaterfall(frame)
        }
      }
    }

    // Listen to background service FSO frames
    viewModelScope.launch {
      SignalTelemetryService.latestFsoPacket.collect { frame ->
        if (frame != null && isBackgroundServiceRunning.value) {
          _fsoFrame.value = frame
        }
      }
    }

    // Listen to network bridge detection events and persist to Room DB
    viewModelScope.launch {
      bridgeClient.eventFlow.collect { event ->
        addEvent(event)
      }
    }

    // Listen to telemetry stream events to record system events in Room DB
    viewModelScope.launch {
      bridgeClient.telemetryEvents.collect { teleEvt ->
        when (teleEvt) {
          is TelemetryStreamEvent.ConnectionStateChanged -> {
            threatLogRepository.recordSystemEvent(
              category = "CONNECTION",
              title = "Connection: ${teleEvt.newStatus.name}",
              message = teleEvt.message,
              severity = if (teleEvt.newStatus == ConnectionStatus.CONNECTED) "SUCCESS" else "INFO",
              targetEndpoint = activeServerUrl.value
            )
          }
          is TelemetryStreamEvent.ErrorOccurred -> {
            threatLogRepository.recordSystemEvent(
              category = "CONNECTION",
              title = "WebSocket Communication Error",
              message = teleEvt.error,
              severity = "ERROR",
              targetEndpoint = activeServerUrl.value
            )
          }
          is TelemetryStreamEvent.ServerStatusUpdated -> {
            threatLogRepository.recordSystemEvent(
              category = "CONFIG",
              title = "GNU Radio Flowgraph Sync",
              message = "Scanning: ${teleEvt.isScanning}, FSO Blocked: ${teleEvt.fsoAllBlocked}",
              severity = "INFO"
            )
          }
          else -> Unit
        }
      }
    }

    // Listen to spatial emitters
    viewModelScope.launch {
      bridgeClient.spatialEmitters.collect { emitters ->
        if (connectionStatus.value == ConnectionStatus.CONNECTED && emitters.isNotEmpty()) {
          _spatialEmitters.value = emitters
        }
      }
    }

    // Listen to modulation classifications
    viewModelScope.launch {
      bridgeClient.modulationClassification.collect { clf ->
        if (connectionStatus.value == ConnectionStatus.CONNECTED && clf.modulation.isNotEmpty()) {
          _modulationClassification.value = clf
          _candidateModulations.value = listOf(clf.modulation to clf.confidence)
        }
      }
    }

    // Listen to decoded frames
    viewModelScope.launch {
      bridgeClient.decodedFrames.collect { frames ->
        if (connectionStatus.value == ConnectionStatus.CONNECTED && frames.isNotEmpty()) {
          _decodedFrames.value = frames
        }
      }
    }

    // Start simulation loop (default out-of-the-box or fallback)
    startSimulationLoop()
  }

  fun setBridgeHost(host: String) {
    _bridgeHost.value = host
  }

  fun setBridgePort(port: String) {
    _bridgePort.value = port
  }

  fun selectRfGroup(group: String) {
    _selectedRfGroup.value = group
  }

  fun setLogSearchQuery(query: String) {
    _logSearchQuery.value = query
  }

  fun setLogFilterType(filter: String) {
    _logFilterType.value = filter
  }

  fun setActiveLogViewTab(tabIndex: Int) {
    _activeLogViewTab.value = tabIndex
  }

  fun refreshNetworkInfo() {
    val ip = com.example.util.NetworkUtils.getDeviceLocalIpAddress(getApplication())
    _deviceLocalIp.value = ip
    _subnetPrefix.value = com.example.util.NetworkUtils.getSubnetPrefix(ip)
  }

  fun probeEndpoint(host: String = _bridgeHost.value, port: Int = _bridgePort.value.toIntOrNull() ?: 8765) {
    if (_isProbing.value) return
    _isProbing.value = true
    _probeStatusMessage.value = "Testing socket reachability on $host:$port..."

    viewModelScope.launch {
      val result = bridgeClient.probeTcpPort(host, port)
      _probeStatusMessage.value = result
      _isProbing.value = false

      threatLogRepository.recordSystemEvent(
        category = "CONNECTION",
        title = "TCP Socket Probe",
        message = "$host:$port -> $result",
        severity = if (result.startsWith("SUCCESS")) "SUCCESS" else "WARNING",
        targetEndpoint = "$host:$port"
      )
    }
  }

  fun connectViaAdbReverse(port: Int = 8765) {
    _bridgeHost.value = "ws://127.0.0.1:$port"
    _bridgePort.value = port.toString()
    connectToBridge()
  }

  fun connectViaLanIp(hostIp: String, port: Int = 8765) {
    val cleanIp = hostIp.trim().removePrefix("ws://").removePrefix("http://")
    _bridgeHost.value = if (cleanIp.startsWith("ws://")) cleanIp else "ws://$cleanIp"
    _bridgePort.value = port.toString()
    connectToBridge()
  }

  fun applyPreset(preset: ServerPreset) {
    _selectedPreset.value = preset
    _bridgeHost.value = preset.url
    _bridgePort.value = preset.defaultPort.toString()
  }

  fun connectToPreset(preset: ServerPreset) {
    applyPreset(preset)
    bridgeClient.connect(preset.url, preset.defaultPort, "${preset.institution} - ${preset.title}")
    viewModelScope.launch(Dispatchers.IO) {
      threatLogRepository.recordSystemEvent(
        category = "CONNECTION",
        title = "Connecting to ${preset.title}",
        message = "Initiated WebSocket handshake with endpoint: ${preset.url}",
        severity = "INFO",
        targetEndpoint = preset.url
      )
    }
  }

  fun connectToBridge() {
    val port = _bridgePort.value.toIntOrNull() ?: 8765
    val presetName = _selectedPreset.value?.let { "${it.institution} - ${it.title}" } ?: "GNU Radio Custom WebSocket"
    bridgeClient.connect(_bridgeHost.value, port, presetName)
    viewModelScope.launch(Dispatchers.IO) {
      threatLogRepository.recordSystemEvent(
        category = "CONNECTION",
        title = "Connecting to GNU Radio Bridge",
        message = "Target: ${_bridgeHost.value}:$port",
        severity = "INFO",
        targetEndpoint = _bridgeHost.value
      )
    }
  }

  fun disconnectBridge() {
    bridgeClient.disconnect()
    viewModelScope.launch(Dispatchers.IO) {
      threatLogRepository.recordSystemEvent(
        category = "CONNECTION",
        title = "WebSocket Disconnected",
        message = "User manually terminated active telemetry stream socket.",
        severity = "WARNING"
      )
    }
  }

  // --- BACKGROUND WEBSOCKET FETCH SERVICE CONTROLS ---
  fun startBackgroundFetchService(intervalMs: Long = bgFetchIntervalMs.value) {
    val port = _bridgePort.value.toIntOrNull() ?: 8765
    SignalTelemetryService.start(
      context = getApplication(),
      url = _bridgeHost.value,
      port = port,
      intervalMs = intervalMs
    )
    viewModelScope.launch(Dispatchers.IO) {
      threatLogRepository.recordSystemEvent(
        category = "BACKGROUND_SERVICE",
        title = "Background WebSocket Service Activated",
        message = "Periodic RF & FSO signal packet fetching started (every ${intervalMs}ms). Host: ${_bridgeHost.value}:$port",
        severity = "SUCCESS",
        targetEndpoint = "${_bridgeHost.value}:$port"
      )
    }
  }

  fun stopBackgroundFetchService() {
    SignalTelemetryService.stop(getApplication())
    viewModelScope.launch(Dispatchers.IO) {
      threatLogRepository.recordSystemEvent(
        category = "BACKGROUND_SERVICE",
        title = "Background WebSocket Service Terminated",
        message = "Background telemetry packet fetching stopped by operator.",
        severity = "INFO"
      )
    }
  }

  fun setBackgroundFetchInterval(intervalMs: Long) {
    SignalTelemetryService.updateInterval(getApplication(), intervalMs)
  }

  fun fetchSignalPacketsNow() {
    if (isBackgroundServiceRunning.value) {
      SignalTelemetryService.fetchNow(getApplication())
    } else {
      if (connectionStatus.value == ConnectionStatus.CONNECTED) {
        bridgeClient.sendPollTelemetry(listOf("RF", "FSO"))
        bridgeClient.sendFetchRfPacket()
        bridgeClient.sendFetchFsoPacket()
      } else {
        val (rf, rfEvt) = simulationEngine.generateRfFrame(_shieldConfig.value)
        val (fso, fsoEvt) = simulationEngine.generateFsoFrame(_shieldConfig.value)
        _rfFrame.value = rf
        _fsoFrame.value = fso
        updateWaterfall(rf)
        if (rfEvt != null) addEvent(rfEvt)
        if (fsoEvt != null) addEvent(fsoEvt)
      }
    }
    viewModelScope.launch(Dispatchers.IO) {
      threatLogRepository.recordSystemEvent(
        category = "PACKET_FETCH",
        title = "Manual Packet Query Dispatched",
        message = "Polled RF spectrum & FSO optical pipelines out-of-band.",
        severity = "INFO"
      )
    }
  }

  fun enableSimulationMode() {
    // If strict FCC regulation is enforced, disallow simulation and warn operator
    if (_systemSettings.value.fccConfig.enforceNonSimulatedHardware) {
      viewModelScope.launch(Dispatchers.IO) {
        threatLogRepository.recordSystemEvent(
          category = "FCC_REGULATION",
          title = "Simulation Mode Blocked by FCC Rules",
          message = "FCC Part 15 / Part 95 mandate strictly requires live certified SDR/SDR-bridge hardware. Disable strict non-simulated mode in Settings if operating on bench testbed.",
          severity = "WARNING"
        )
      }
      return
    }
    bridgeClient.switchToSimulation()
    if (simLoopJob == null || !simLoopJob!!.isActive) {
      startSimulationLoop()
    }
    viewModelScope.launch(Dispatchers.IO) {
      threatLogRepository.recordSystemEvent(
        category = "CONFIG",
        title = "Simulation Mode Activated",
        message = "Switched to local GNU Radio emulation & signal synthesis engine.",
        severity = "INFO"
      )
    }
  }

  // --- SETTINGS, LOCATION, EMERGENCY SERVICES & HIPAA GOVERNANCE ---
  fun toggleGpsLocationTracking(enabled: Boolean) {
    val currentLoc = _systemSettings.value.locationSettings
    _systemSettings.value = _systemSettings.value.copy(
      locationSettings = currentLoc.copy(gpsLocationEnabled = enabled)
    )
    viewModelScope.launch(Dispatchers.IO) {
      threatLogRepository.recordSystemEvent(
        category = "LOCATION_SERVICE",
        title = if (enabled) "GPS Emergency Geofence Tracking Activated" else "GPS Location Tracking Disabled",
        message = "Operator toggled biomedical device coordinates beaconing to: $enabled",
        severity = "INFO"
      )
    }
  }

  fun toggleEmergencyLocationSharing(enabled: Boolean) {
    val currentLoc = _systemSettings.value.locationSettings
    _systemSettings.value = _systemSettings.value.copy(
      locationSettings = currentLoc.copy(emergencyLocationSharingActive = enabled)
    )
  }

  fun updateFccEnforcement(enforceNonSimulated: Boolean, strictShielding: Boolean, autoReport: Boolean) {
    val currentFcc = _systemSettings.value.fccConfig
    _systemSettings.value = _systemSettings.value.copy(
      fccConfig = currentFcc.copy(
        enforceNonSimulatedHardware = enforceNonSimulated,
        strictInterferenceShielding = strictShielding,
        autoReportRogueTransmitters = autoReport
      )
    )
    viewModelScope.launch(Dispatchers.IO) {
      threatLogRepository.recordSystemEvent(
        category = "FCC_REGULATION",
        title = "FCC Telecommunications Policy Updated",
        message = "Enforce Real Hardware: $enforceNonSimulated | Strict Interference Shielding: $strictShielding | Auto-Report Violations: $autoReport",
        severity = "SUCCESS"
      )
    }
  }

  fun toggleHospitalViewingAuthorization(hospitalId: String, granted: Boolean) {
    val updatedAuths = _systemSettings.value.hospitalAuthorizations.map { auth ->
      if (auth.hospitalId == hospitalId) {
        auth.copy(
          thirdPartyViewingPermitted = granted,
          remoteMitigationPermitted = granted,
          signalLocationTrackingPermitted = granted,
          hipaaBaaStatus = if (granted) "ACTIVE_BAA_SIGNED" else "REVOKED"
        )
      } else auth
    }
    _systemSettings.value = _systemSettings.value.copy(hospitalAuthorizations = updatedAuths)

    // Sync to device fleet
    val currentFleet = _biomedicalDevices.value.map { dev ->
      if (dev.hospitalId == hospitalId) {
        dev.copy(hospitalAuthorizationGranted = granted)
      } else dev
    }
    _biomedicalDevices.value = currentFleet

    viewModelScope.launch(Dispatchers.IO) {
      threatLogRepository.recordSystemEvent(
        category = "HIPAA_AUTHORIZATION",
        title = if (granted) "Hospital Telemetry Access Granted" else "Hospital Telemetry Access Revoked",
        message = "Hospital ID: $hospitalId | Third-party device viewing & location tracking set to: $granted",
        severity = if (granted) "SUCCESS" else "WARNING"
      )
    }
  }

  fun toggleHipaaMasking(masked: Boolean) {
    _systemSettings.value = _systemSettings.value.copy(hipaaMaskingEnabled = masked)
    viewModelScope.launch(Dispatchers.IO) {
      threatLogRepository.recordSystemEvent(
        category = "HIPAA_AUTHORIZATION",
        title = if (masked) "Strict HIPAA Patient Name Masking Enforced" else "Patient Direct Telemetry Viewing Unmasked",
        message = "Masked mode hides patient names unless explicitly permitted by hospital BAA.",
        severity = "INFO"
      )
    }
  }

  fun addEmergencyContact(contact: EmergencyContact) {
    val currentContacts = _systemSettings.value.emergencyContacts.toMutableList()
    currentContacts.add(contact)
    _systemSettings.value = _systemSettings.value.copy(emergencyContacts = currentContacts)
    viewModelScope.launch(Dispatchers.IO) {
      threatLogRepository.recordSystemEvent(
        category = "EMERGENCY_SERVICE",
        title = "Emergency Dispatch Agency Added",
        message = "Added: ${contact.serviceName} (${contact.phoneNumber})",
        severity = "SUCCESS"
      )
    }
  }

  fun removeEmergencyContact(contactId: String) {
    val filtered = _systemSettings.value.emergencyContacts.filterNot { it.id == contactId }
    _systemSettings.value = _systemSettings.value.copy(emergencyContacts = filtered)
  }

  fun toggleScanning() {
    val next = !_isScanning.value
    _isScanning.value = next
    if (connectionStatus.value == ConnectionStatus.CONNECTED) {
      if (next) bridgeClient.sendStartScan() else bridgeClient.sendStopScan()
    }
    viewModelScope.launch(Dispatchers.IO) {
      threatLogRepository.recordSystemEvent(
        category = "SCAN",
        title = if (next) "Spectrum Sweep Resumed" else "Spectrum Sweep Paused",
        message = "Scanning status toggled to: $next",
        severity = "INFO"
      )
    }
  }

  fun updateRfThresholds(alert: Float, high: Float) {
    _shieldConfig.value = _shieldConfig.value.copy(rfAlertDbm = alert, rfHighDbm = high)
    if (connectionStatus.value == ConnectionStatus.CONNECTED) {
      bridgeClient.sendRfThresholds(alert, high)
    }
    viewModelScope.launch(Dispatchers.IO) {
      threatLogRepository.recordSystemEvent(
        category = "CONFIG",
        title = "RF Shield Thresholds Updated",
        message = "RF Alert: $alert dBm, RF Critical High: $high dBm",
        severity = "INFO"
      )
    }
  }

  fun updateFsoThresholds(alert: Float, block: Float) {
    _shieldConfig.value = _shieldConfig.value.copy(fsoAlertDbm = alert, fsoBlockDbm = block)
    if (connectionStatus.value == ConnectionStatus.CONNECTED) {
      bridgeClient.sendFsoThresholds(alert, block)
    }
    viewModelScope.launch(Dispatchers.IO) {
      threatLogRepository.recordSystemEvent(
        category = "CONFIG",
        title = "FSO Optical Thresholds Updated",
        message = "FSO Alert: $alert dBm, FSO Auto-Block: $block dBm",
        severity = "INFO"
      )
    }
  }

  fun toggleAutoBlockFso(enabled: Boolean) {
    _shieldConfig.value = _shieldConfig.value.copy(autoBlockFso = enabled)
  }

  fun toggleSoundAlarm(enabled: Boolean) {
    _shieldConfig.value = _shieldConfig.value.copy(soundAlarm = enabled)
  }

  fun toggleFsoBandShutter(bandName: String) {
    val currentFso = _fsoFrame.value
    val band = currentFso.bands.find { it.name == bandName }
    val willBlock = !(band?.blocked ?: false)

    if (connectionStatus.value == ConnectionStatus.CONNECTED) {
      if (willBlock) {
        bridgeClient.sendFsoBlock(bandName)
      } else {
        bridgeClient.sendFsoUnblock(bandName)
      }
    } else {
      simulationEngine.setFsoBandBlocked(bandName, willBlock)
    }

    viewModelScope.launch(Dispatchers.IO) {
      threatLogRepository.recordSystemEvent(
        category = "SHIELD",
        title = if (willBlock) "Optical Shutter Closed: $bandName" else "Optical Shutter Opened: $bandName",
        message = "Manual shutter actuation executed for $bandName.",
        severity = if (willBlock) "WARNING" else "SUCCESS"
      )
    }
  }

  fun blockAllFsoShutters() {
    if (connectionStatus.value == ConnectionStatus.CONNECTED) {
      bridgeClient.sendFsoBlockAll()
    } else {
      simulationEngine.setAllFsoBlocked(true)
    }

    viewModelScope.launch(Dispatchers.IO) {
      threatLogRepository.recordSystemEvent(
        category = "SHIELD",
        title = "ALL OPTICAL SHUTTERS ENGAGED",
        message = "Emergency optical block triggered across all spectral channels (850nm, 905nm, 1064nm, 1550nm).",
        severity = "WARNING"
      )
    }
  }

  fun resetAllFsoShutters() {
    if (connectionStatus.value == ConnectionStatus.CONNECTED) {
      bridgeClient.sendFsoReset()
    } else {
      simulationEngine.setAllFsoBlocked(false)
    }

    viewModelScope.launch(Dispatchers.IO) {
      threatLogRepository.recordSystemEvent(
        category = "SHIELD",
        title = "Optical Shutters Reset",
        message = "All optical shutters reopened for nominal beam telemetry reception.",
        severity = "INFO"
      )
    }
  }

  fun selectEmitter(emitter: SpatialEmitter) {
    _selectedEmitter.value = if (_selectedEmitter.value?.id == emitter.id) null else emitter
  }

  fun clearFrames() {
    _decodedFrames.value = emptyList()
  }

  fun dismissThreatBanner() {
    _activeThreatBanner.value = null
  }

  fun clearEvents() {
    _events.value = emptyList()
    _activeThreatBanner.value = null
  }

  // --- Room Database Management Actions ---
  fun deleteThreatLog(id: Long) {
    viewModelScope.launch(Dispatchers.IO) {
      threatLogRepository.deleteThreat(id)
    }
  }

  fun clearAllThreatLogs() {
    viewModelScope.launch(Dispatchers.IO) {
      threatLogRepository.clearAllThreats()
      threatLogRepository.recordSystemEvent(
        category = "CONFIG",
        title = "Threat History Cleared",
        message = "Room database threat history audit table purged by operator.",
        severity = "INFO"
      )
    }
  }

  fun deleteSystemEventLog(id: Long) {
    viewModelScope.launch(Dispatchers.IO) {
      threatLogRepository.deleteSystemEvent(id)
    }
  }

  fun clearAllSystemEventLogs() {
    viewModelScope.launch(Dispatchers.IO) {
      threatLogRepository.clearAllSystemEvents()
    }
  }

  fun deleteBioMetricLog(id: Long) {
    viewModelScope.launch(Dispatchers.IO) {
      threatLogRepository.deleteBioMetricData(id)
    }
  }

  fun clearAllBioMetricLogs() {
    viewModelScope.launch(Dispatchers.IO) {
      threatLogRepository.clearAllBioMetricData()
      threatLogRepository.recordSystemEvent(
        category = "CONFIG",
        title = "BioMetric Telemetry Logs Cleared",
        message = "Room database physiological telemetry audit trail cleared by operator.",
        severity = "INFO"
      )
    }
  }

  fun recordBioMetricAudit(device: BiomedicalDevice, notes: String = "Manual operator audit checkpoint.") {
    viewModelScope.launch(Dispatchers.IO) {
      threatLogRepository.recordBioMetricTelemetry(device, notes)
    }
  }

  fun toggleAutoScanAnomalies(enabled: Boolean) {
    _autoScanAnomalies.value = enabled
  }

  fun analyzeStreamWithGemini(force: Boolean = false) {
    val now = System.currentTimeMillis()
    if (isAnalysisRunning && !force) return
    // Debounce rapid repeated automated scans (min 6s unless force manual action)
    if (!force && (now - lastAnalysisTimestamp < 6000L)) return

    lastAnalysisTimestamp = now
    isAnalysisRunning = true
    _streamAnomalyReport.value = _streamAnomalyReport.value.copy(isAnalyzing = true)

    viewModelScope.launch {
      try {
        val currentServer = if (connectionStatus.value == ConnectionStatus.CONNECTED) {
          activeServerUrl.value.ifEmpty { _bridgeHost.value }
        } else {
          "GNU Radio Simulation Engine (WebSocket Protocol)"
        }

        val report = geminiThreatClassifier.classifyStreamAnomaly(
          rfFrame = _rfFrame.value,
          fsoFrame = _fsoFrame.value,
          spatialEmitters = _spatialEmitters.value,
          modulation = _modulationClassification.value,
          decodedFrames = _decodedFrames.value,
          events = _events.value,
          serverUrl = currentServer
        )
        _streamAnomalyReport.value = report

        // Persist AI analysis result to Room Database System Events
        threatLogRepository.recordSystemEvent(
          category = "AI_ANALYSIS",
          title = "SigInt Threat Report [${report.threatCategoryText.uppercase()}]",
          message = "${report.interferenceType}: ${report.summary}",
          severity = when (report.threatCategory) {
            InterferenceCategory.HIGH -> "ERROR"
            InterferenceCategory.MEDIUM -> "WARNING"
            InterferenceCategory.LOW -> "SUCCESS"
          },
          targetEndpoint = currentServer
        )
      } catch (e: Exception) {
        _streamAnomalyReport.value = _streamAnomalyReport.value.copy(
          isAnalyzing = false,
          errorMessage = e.message
        )
      } finally {
        isAnalysisRunning = false
      }
    }
  }

  fun triggerSimulatedLaserAttack() {
    simulationEngine.triggerIncursion("HIGH_POWER_PULSED_LASER", "1550nm C-Band", 35)
    if (_autoScanAnomalies.value) {
      analyzeStreamWithGemini(force = false)
    }
  }

  fun triggerSimulatedRogueJammer() {
    simulationEngine.triggerIncursion("BROADBAND_RF_JAMMER", "Wi-Fi 2.4G Ch 6", 35)
    if (_autoScanAnomalies.value) {
      analyzeStreamWithGemini(force = false)
    }
  }

  fun triggerSimulatedLidarSpoofer() {
    simulationEngine.triggerIncursion("PULSED_LIDAR_SPOOFER", "905nm Pulsed NIR", 35)
    if (_autoScanAnomalies.value) {
      analyzeStreamWithGemini(force = false)
    }
  }

  fun generateExportJson(): String {
    val rf = _rfFrame.value
    val fso = _fsoFrame.value
    val evts = _events.value
    val persistedCount = persistentThreatCount.value
    val bioCount = bioMetricCount.value
    val sysCount = systemEventCount.value

    val builder = StringBuilder()
    builder.append("{\n")
    builder.append("  \"shield_system\": \"Unified Signal Shield Telemetry & BioMetric Audit Export\",\n")
    builder.append("  \"database\": \"Room SQLite Database Schema v2\",\n")
    builder.append("  \"timestamp\": ${System.currentTimeMillis()},\n")
    builder.append("  \"status\": {\n")
    builder.append("    \"scanning\": ${_isScanning.value},\n")
    builder.append("    \"connection\": \"${connectionStatus.value}\",\n")
    builder.append("    \"latency_ms\": ${latencyMs.value}\n")
    builder.append("  },\n")
    builder.append("  \"rf_peak_power_dbm\": ${rf.peakPowerDbm},\n")
    builder.append("  \"rf_peak_band\": \"${rf.peakBand}\",\n")
    builder.append("  \"fso_all_blocked\": ${fso.allBlocked},\n")
    builder.append("  \"fso_attenuation_db\": ${fso.overallAttenuationDb},\n")
    builder.append("  \"active_session_threats\": ${evts.size},\n")
    builder.append("  \"persisted_room_threat_records\": $persistedCount,\n")
    builder.append("  \"persisted_room_system_events\": $sysCount,\n")
    builder.append("  \"persisted_biometric_audit_records\": $bioCount\n")
    builder.append("}")
    return builder.toString()
  }

  private fun startSimulationLoop() {
    simLoopJob?.cancel()
    simLoopJob = viewModelScope.launch(Dispatchers.Default) {
      while (isActive) {
        if (_isScanning.value && connectionStatus.value != ConnectionStatus.CONNECTED) {
          val (rf, rfEvt) = simulationEngine.generateRfFrame(_shieldConfig.value)
          val (fso, fsoEvt) = simulationEngine.generateFsoFrame(_shieldConfig.value)

          _rfFrame.value = rf
          _fsoFrame.value = fso

          _spatialEmitters.value = simulationEngine.generateSpatialEmitters()
          _opticalTarget.value = simulationEngine.generateOpticalAoATarget()

          val (clf, candidates) = simulationEngine.generateModulationClassification()
          _modulationClassification.value = clf
          _candidateModulations.value = candidates

          if (_decodedFrames.value.isEmpty() || System.currentTimeMillis() % 2000L < 150L) {
            _decodedFrames.value = simulationEngine.generateDecodedFrames()
          }

          val (sigMeta, sigJson) = simulationEngine.generateSigMFMetadata()
          _sigmfMetadata.value = sigMeta
          _sigmfJson.value = sigJson

          updateWaterfall(rf)

          rfEvt?.let { addEvent(it) }
          fsoEvt?.let { addEvent(it) }
        }

        // Periodic Bio-Medical Telemetry & Patient Vitals Refresh (~1 Hz)
        if (System.currentTimeMillis() % 1000L < 150L) {
          val updatedBiomed = biomedicalEngine.tickDevices()
          _biomedicalDevices.value = updatedBiomed
          _selectedBiomedicalDevice.value?.let { currentSel ->
            _selectedBiomedicalDevice.value = updatedBiomed.firstOrNull { it.id == currentSel.id } ?: updatedBiomed.getOrNull(0)
          }
        }

        delay(120) // ~8 fps smooth update
      }
    }
  }

  fun selectBiomedicalDevice(device: BiomedicalDevice) {
    _selectedBiomedicalDevice.value = device
  }

  fun injectBiomedJamming(deviceId: String = "MED-PAC-8041") {
    val mod = biomedicalEngine.injectPacemakerJamming(deviceId)
    mod?.let {
      val updated = biomedicalEngine.tickDevices()
      _biomedicalDevices.value = updated
      _selectedBiomedicalDevice.value = it
      addEvent(
        DetectionEvent(
          id = "THREAT-BIOMED-JAM-${System.currentTimeMillis()}",
          timestamp = System.currentTimeMillis(),
          source = SignalSource.RF,
          band = "402.5 MHz MedRadio",
          powerDbm = -96.0,
          thresholdDbm = -80.0,
          threatType = "MEDRADIO_IMPLANT_JAMMING",
          severity = EventSeverity.CRITICAL,
          details = "Malicious jamming carrier targeted at patient implant ${it.patientName} (${it.id}). 95% packet drop.",
          actionTaken = "Triggered Medical Emergency 911 Dispatch Alert"
        )
      )
      viewModelScope.launch(Dispatchers.IO) {
        threatLogRepository.recordBioMetricTelemetry(it, "AUDIT: MedRadio RF Jamming Injected. Telemetry link degraded.")
      }
    }
  }

  fun injectBiomedArrhythmia(deviceId: String = "MED-PAC-8041") {
    val mod = biomedicalEngine.injectArrhythmiaVfib(deviceId)
    mod?.let {
      val updated = biomedicalEngine.tickDevices()
      _biomedicalDevices.value = updated
      _selectedBiomedicalDevice.value = it
      addEvent(
        DetectionEvent(
          id = "THREAT-BIOMED-VFIB-${System.currentTimeMillis()}",
          timestamp = System.currentTimeMillis(),
          source = SignalSource.RF,
          band = "Cardiac Telemetry",
          powerDbm = -60.0,
          thresholdDbm = -80.0,
          threatType = "ACUTE_VENTRICULAR_FIBRILLATION",
          severity = EventSeverity.CRITICAL,
          details = "Cardiac arrest / V-Fib detected on patient ${it.patientName} (${it.patientRoom}). Urgent Code Blue dispatch required.",
          actionTaken = "Autonomous Crash Cart & Emergency EMS alert dispatched."
        )
      )
      viewModelScope.launch(Dispatchers.IO) {
        threatLogRepository.recordBioMetricTelemetry(it, "AUDIT: Acute Ventricular Fibrillation anomaly logged. Heart rate spike.")
      }
    }
  }

  fun injectBiomedInfusionOverdose(deviceId: String = "MED-PUMP-209") {
    val mod = biomedicalEngine.injectInfusionOverdose(deviceId)
    mod?.let {
      val updated = biomedicalEngine.tickDevices()
      _biomedicalDevices.value = updated
      _selectedBiomedicalDevice.value = it
      addEvent(
        DetectionEvent(
          id = "THREAT-BIOMED-PUMP-${System.currentTimeMillis()}",
          timestamp = System.currentTimeMillis(),
          source = SignalSource.RF,
          band = "2.4 GHz Wi-Fi IoT",
          powerDbm = -58.0,
          thresholdDbm = -80.0,
          threatType = "UNAUTHORIZED_PUMP_TITRATION",
          severity = EventSeverity.CRITICAL,
          details = "Unauthorized remote override on smart infusion pump ${it.id}. Overdose rate 350 mL/h.",
          actionTaken = "Flagged malicious IoT exploit; recommend immediate manual line clamp."
        )
      )
      viewModelScope.launch(Dispatchers.IO) {
        threatLogRepository.recordBioMetricTelemetry(it, "AUDIT: Unauthorized smart pump rate titration detected (350 mL/h).")
      }
    }
  }

  fun injectBiomedVentFailure(deviceId: String = "MED-VENT-550") {
    val mod = biomedicalEngine.injectVentilatorFailure(deviceId)
    mod?.let {
      val updated = biomedicalEngine.tickDevices()
      _biomedicalDevices.value = updated
      _selectedBiomedicalDevice.value = it
      addEvent(
        DetectionEvent(
          id = "THREAT-BIOMED-VENT-${System.currentTimeMillis()}",
          timestamp = System.currentTimeMillis(),
          source = SignalSource.RF,
          band = "5.8 GHz Medical Telemetry",
          powerDbm = -68.0,
          thresholdDbm = -80.0,
          threatType = "VENTILATOR_APNEA_FAILURE",
          severity = EventSeverity.CRITICAL,
          details = "Critical circuit apnea & pressure drop on ventilator ${it.id} (${it.patientName}).",
          actionTaken = "Triggered urgent ICU clinical respiratory intervention alert."
        )
      )
      viewModelScope.launch(Dispatchers.IO) {
        threatLogRepository.recordBioMetricTelemetry(it, "AUDIT: Critical ventilator apnea & pressure failure registered.")
      }
    }
  }

  fun resolveBiomedDevice(deviceId: String) {
    val mod = biomedicalEngine.resolveDevice(deviceId)
    mod?.let {
      val updated = biomedicalEngine.tickDevices()
      _biomedicalDevices.value = updated
      _selectedBiomedicalDevice.value = it
      viewModelScope.launch(Dispatchers.IO) {
        threatLogRepository.recordBioMetricTelemetry(it, "AUDIT: Device anomaly resolved. Vitals returned to baseline.")
        threatLogRepository.recordSystemEvent(
          category = "BIOMED_ALERT",
          title = "Biomedical Alert Resolved: ${it.id}",
          message = "Patient ${it.patientName} telemetry returned to nominal status.",
          severity = "SUCCESS"
        )
      }
    }
  }

  fun restoreAllBiomedDevices() {
    val updated = biomedicalEngine.restoreAllDevices()
    _biomedicalDevices.value = updated
    _selectedBiomedicalDevice.value = updated.getOrNull(0)
    viewModelScope.launch(Dispatchers.IO) {
      updated.forEach { dev ->
        threatLogRepository.recordBioMetricTelemetry(dev, "AUDIT: Fleet reset - Nominal baseline restored.")
      }
      threatLogRepository.recordSystemEvent(
        category = "BIOMED_ALERT",
        title = "All Biomedical Devices Restored",
        message = "All patient telemetry sensors operating normally.",
        severity = "SUCCESS"
      )
    }
  }

  fun dispatchBiomedEmergency(device: BiomedicalDevice): MedicalEmergencyDispatch {
    val report = biomedicalEngine.createEmergencyReport(device)
    viewModelScope.launch(Dispatchers.IO) {
      threatLogRepository.recordBioMetricTelemetry(
        device,
        "AUDIT: 🚨 EMERGENCY DISPATCH (CAD ${report.incidentId}). Reason: ${report.failureReason}"
      )
      threatLogRepository.recordSystemEvent(
        category = "EMERGENCY_DISPATCH",
        title = "🚨 911 / CODE BLUE DISPATCHED: ${device.patientName}",
        message = "CAD ID: ${report.incidentId} | Reason: ${report.failureReason} | Location: ${report.location}",
        severity = "ERROR"
      )
    }
    return report
  }

  private fun updateWaterfall(rf: RFSpectrumFrame) {
    val selectedGroup = _selectedRfGroup.value
    val spec = rf.spectra[selectedGroup] ?: rf.spectra.values.firstOrNull() ?: return
    val psd = spec.psd
    if (psd.isEmpty()) return

    val floatPowers = FloatArray(psd.size) { i -> psd[i].toFloat() }
    val newRow = WaterfallRow(rf.timestamp, floatPowers)

    val currentList = _waterfallHistory.value.toMutableList()
    currentList.add(0, newRow) // newest row on top
    if (currentList.size > 50) {
      currentList.removeAt(currentList.size - 1)
    }
    _waterfallHistory.value = currentList
  }

  private fun addEvent(event: DetectionEvent) {
    val current = _events.value.toMutableList()
    current.add(0, event)
    if (current.size > 100) {
      current.removeAt(current.size - 1)
    }
    _events.value = current
    _activeThreatBanner.value = event

    // Automatically persist detected threat incident to Room Database
    viewModelScope.launch(Dispatchers.IO) {
      threatLogRepository.recordThreat(event)
    }

    if (_autoScanAnomalies.value && (event.severity == EventSeverity.CRITICAL || event.severity == EventSeverity.HIGH)) {
      analyzeStreamWithGemini(force = false)
    }
  }
}
