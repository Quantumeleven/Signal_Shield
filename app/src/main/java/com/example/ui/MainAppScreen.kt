package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.EventSeverity
import com.example.ui.components.TacticalTopBar
import com.example.ui.components.ThreatAlertBanner
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.viewmodel.SignalViewModel

enum class NavigationTab(
  val label: String,
  val icon: ImageVector
) {
  HUD("HUD", Icons.Default.Shield),
  BIOMED("BIOMED", Icons.Default.MonitorHeart),
  RADAR("AoA RADAR", Icons.Default.GpsFixed),
  DECODER("AMR/SNIFF", Icons.Default.Psychology),
  RF("RF SPEC", Icons.Default.Wifi),
  FSO("FSO OPT", Icons.Default.LightMode),
  EVENTS("LOGS", Icons.Default.Assessment),
  CONFIG("CONFIG", Icons.Default.Tune),
  SETTINGS("SETTINGS", Icons.Default.Settings)
}

@Composable
fun MainAppScreen(
  viewModel: SignalViewModel,
  modifier: Modifier = Modifier
) {
  var currentTab by remember { mutableStateOf(NavigationTab.HUD) }

  val connectionStatus by viewModel.connectionStatus.collectAsStateWithLifecycle()
  val latencyMs by viewModel.latencyMs.collectAsStateWithLifecycle()
  val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
  val shieldConfig by viewModel.shieldConfig.collectAsStateWithLifecycle()
  val rfFrame by viewModel.rfFrame.collectAsStateWithLifecycle()
  val fsoFrame by viewModel.fsoFrame.collectAsStateWithLifecycle()
  val events by viewModel.events.collectAsStateWithLifecycle()
  val waterfallHistory by viewModel.waterfallHistory.collectAsStateWithLifecycle()
  val selectedRfGroup by viewModel.selectedRfGroup.collectAsStateWithLifecycle()
  val bridgeHost by viewModel.bridgeHost.collectAsStateWithLifecycle()
  val bridgePort by viewModel.bridgePort.collectAsStateWithLifecycle()
  val activeServerUrl by viewModel.activeServerUrl.collectAsStateWithLifecycle()
  val serverDescription by viewModel.serverDescription.collectAsStateWithLifecycle()
  val lastLogMessage by viewModel.lastLogMessage.collectAsStateWithLifecycle()
  val selectedPreset by viewModel.selectedPreset.collectAsStateWithLifecycle()
  val activeThreatBanner by viewModel.activeThreatBanner.collectAsStateWithLifecycle()

  // New AoA, RadioML, and Protocol Sniffer States
  val spatialEmitters by viewModel.spatialEmitters.collectAsStateWithLifecycle()
  val opticalTarget by viewModel.opticalTarget.collectAsStateWithLifecycle()
  val selectedEmitter by viewModel.selectedEmitter.collectAsStateWithLifecycle()
  val modulationClassification by viewModel.modulationClassification.collectAsStateWithLifecycle()
  val candidateModulations by viewModel.candidateModulations.collectAsStateWithLifecycle()
  val decodedFrames by viewModel.decodedFrames.collectAsStateWithLifecycle()
  val sigmfMetadata by viewModel.sigmfMetadata.collectAsStateWithLifecycle()
  val sigmfJson by viewModel.sigmfJson.collectAsStateWithLifecycle()
  val streamAnomalyReport by viewModel.streamAnomalyReport.collectAsStateWithLifecycle()
  val autoScanAnomalies by viewModel.autoScanAnomalies.collectAsStateWithLifecycle()

  // Bio-Medical Device Fleet States
  val biomedicalDevices by viewModel.biomedicalDevices.collectAsStateWithLifecycle()
  val selectedBiomedicalDevice by viewModel.selectedBiomedicalDevice.collectAsStateWithLifecycle()

  // Room Database State Flows
  val persistedThreats by viewModel.persistedThreats.collectAsStateWithLifecycle()
  val persistedSystemEvents by viewModel.persistedSystemEvents.collectAsStateWithLifecycle()
  val persistedBioMetrics by viewModel.persistedBioMetrics.collectAsStateWithLifecycle()
  val logSearchQuery by viewModel.logSearchQuery.collectAsStateWithLifecycle()
  val logFilterType by viewModel.logFilterType.collectAsStateWithLifecycle()
  val activeLogViewTab by viewModel.activeLogViewTab.collectAsStateWithLifecycle()

  val isRunningOnEmulator = viewModel.isRunningOnEmulator
  val deviceLocalIp by viewModel.deviceLocalIp.collectAsStateWithLifecycle()
  val subnetPrefix by viewModel.subnetPrefix.collectAsStateWithLifecycle()
  val probeStatusMessage by viewModel.probeStatusMessage.collectAsStateWithLifecycle()
  val isProbing by viewModel.isProbing.collectAsStateWithLifecycle()

  // Background WebSocket Service State
  val isBackgroundServiceRunning by viewModel.isBackgroundServiceRunning.collectAsStateWithLifecycle()
  val bgRfPacketsFetched by viewModel.bgRfPacketsFetched.collectAsStateWithLifecycle()
  val bgFsoPacketsFetched by viewModel.bgFsoPacketsFetched.collectAsStateWithLifecycle()
  val bgLastFetchTimestamp by viewModel.bgLastFetchTimestamp.collectAsStateWithLifecycle()
  val bgFetchIntervalMs by viewModel.bgFetchIntervalMs.collectAsStateWithLifecycle()
  val bgServiceLog by viewModel.bgServiceLog.collectAsStateWithLifecycle()

  // Settings, FCC Regulation & HIPAA Governance
  val systemSettings by viewModel.systemSettings.collectAsStateWithLifecycle()

  val criticalCount = persistedThreats.count { it.severity == "CRITICAL" }.coerceAtLeast(events.count { it.severity == EventSeverity.CRITICAL })


  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .background(ObsidianBlack),
    topBar = {
      Column(modifier = Modifier.statusBarsPadding()) {
        TacticalTopBar(
          connectionStatus = connectionStatus,
          latencyMs = latencyMs,
          isScanning = isScanning,
          threatCount = criticalCount,
          onToggleScanning = { viewModel.toggleScanning() },
          onOpenSettings = { currentTab = NavigationTab.SETTINGS }
        )

        activeThreatBanner?.let { bannerEvent ->
          ThreatAlertBanner(
            event = bannerEvent,
            onDismiss = { viewModel.dismissThreatBanner() },
            onViewDetails = { currentTab = NavigationTab.EVENTS }
          )
        }
      }
    },
    bottomBar = {
      NavigationBar(
        containerColor = SurfaceDark,
        tonalElevation = 4.dp,
        modifier = Modifier
          .navigationBarsPadding()
          .border(0.5.dp, CardBorder, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
          .testTag("main_navigation_bar")
      ) {
        NavigationTab.values().forEach { tab ->
          val isSelected = currentTab == tab
          NavigationBarItem(
            selected = isSelected,
            onClick = { currentTab = tab },
            icon = {
              BadgedBox(
                badge = {
                  if (tab == NavigationTab.EVENTS && criticalCount > 0) {
                    Badge(
                      containerColor = LaserCrimson,
                      contentColor = Color.White
                    ) {
                      Text("$criticalCount", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp))
                    }
                  }
                }
              ) {
                Icon(
                  imageVector = tab.icon,
                  contentDescription = tab.label,
                  modifier = Modifier.size(19.dp)
                )
              }
            },
            label = {
              Text(
                text = tab.label,
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 8.5.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                  fontFamily = FontFamily.Monospace
                ),
                maxLines = 1
              )
            },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = NeonCyan,
              selectedTextColor = NeonCyan,
              indicatorColor = CardSurfaceVariant,
              unselectedIconColor = TextMuted,
              unselectedTextColor = TextMuted
            ),
            modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
          )
        }
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      when (currentTab) {
        NavigationTab.HUD -> {
          DefenseHudScreen(
            rfFrame = rfFrame,
            fsoFrame = fsoFrame,
            shieldConfig = shieldConfig,
            isScanning = isScanning,
            events = events,
            anomalyReport = streamAnomalyReport,
            connectionStatus = connectionStatus,
            activeServerUrl = activeServerUrl,
            latencyMs = latencyMs,
            autoScanEnabled = autoScanAnomalies,
            onTriggerGeminiScan = { viewModel.analyzeStreamWithGemini(force = true) },
            onToggleAutoScan = { viewModel.toggleAutoScanAnomalies(it) },
            onMitigateThreat = {
              viewModel.blockAllFsoShutters()
              viewModel.updateRfThresholds(alert = -85f, high = -65f)
            },
            onToggleScanning = { viewModel.toggleScanning() },
            onBlockAllFso = { viewModel.blockAllFsoShutters() },
            onResetFso = { viewModel.resetAllFsoShutters() },
            onToggleFsoShutter = { viewModel.toggleFsoBandShutter(it) },
            onTriggerTestLaser = { viewModel.triggerSimulatedLaserAttack() },
            onNavigateToRf = { currentTab = NavigationTab.RF },
            onNavigateToFso = { currentTab = NavigationTab.FSO }
          )
        }

        NavigationTab.BIOMED -> {
          BiomedicalScreen(
            devices = biomedicalDevices,
            selectedDevice = selectedBiomedicalDevice,
            onSelectDevice = { viewModel.selectBiomedicalDevice(it) },
            onInjectJamming = { viewModel.injectBiomedJamming(it) },
            onInjectArrhythmia = { viewModel.injectBiomedArrhythmia(it) },
            onInjectInfusionOverdose = { viewModel.injectBiomedInfusionOverdose(it) },
            onInjectVentFailure = { viewModel.injectBiomedVentFailure(it) },
            onResolveDevice = { viewModel.resolveBiomedDevice(it) },
            onRestoreAll = { viewModel.restoreAllBiomedDevices() },
            onDispatchEmergency = { viewModel.dispatchBiomedEmergency(it) },
            hipaaMaskingEnabled = systemSettings.hipaaMaskingEnabled,
            onNavigateToSettings = { currentTab = NavigationTab.SETTINGS }
          )
        }

        NavigationTab.RADAR -> {
          DirectionFindingScreen(
            emitters = spatialEmitters,
            opticalTarget = opticalTarget,
            selectedEmitter = selectedEmitter,
            onSelectEmitter = { viewModel.selectEmitter(it) }
          )
        }

        NavigationTab.DECODER -> {
          DecoderAnalyticsScreen(
            classification = modulationClassification,
            candidateList = candidateModulations,
            decodedFrames = decodedFrames,
            sigmfMetadata = sigmfMetadata,
            sigmfJson = sigmfJson,
            onClearFrames = { viewModel.clearFrames() }
          )
        }

        NavigationTab.RF -> {
          RfSpectrumScreen(
            rfFrame = rfFrame,
            waterfallHistory = waterfallHistory,
            shieldConfig = shieldConfig,
            selectedGroup = selectedRfGroup,
            onSelectGroup = { viewModel.selectRfGroup(it) }
          )
        }

        NavigationTab.FSO -> {
          FsoOpticalScreen(
            fsoFrame = fsoFrame,
            shieldConfig = shieldConfig,
            onToggleShutter = { viewModel.toggleFsoBandShutter(it) },
            onBlockAll = { viewModel.blockAllFsoShutters() },
            onResetAll = { viewModel.resetAllFsoShutters() },
            onToggleAutoShield = { viewModel.toggleAutoBlockFso(it) }
          )
        }

        NavigationTab.EVENTS -> {
          ThreatLogScreen(
            persistedThreats = persistedThreats,
            persistedSystemEvents = persistedSystemEvents,
            persistedBioMetrics = persistedBioMetrics,
            searchQuery = logSearchQuery,
            selectedFilter = logFilterType,
            activeTab = activeLogViewTab,
            onSearchQueryChanged = { viewModel.setLogSearchQuery(it) },
            onFilterChanged = { viewModel.setLogFilterType(it) },
            onTabChanged = { viewModel.setActiveLogViewTab(it) },
            onDeleteThreat = { viewModel.deleteThreatLog(it) },
            onClearAllThreats = { viewModel.clearAllThreatLogs() },
            onDeleteSystemEvent = { viewModel.deleteSystemEventLog(it) },
            onClearAllSystemEvents = { viewModel.clearAllSystemEventLogs() },
            onDeleteBioMetric = { viewModel.deleteBioMetricLog(it) },
            onClearAllBioMetrics = { viewModel.clearAllBioMetricLogs() },
            onExportJson = { viewModel.generateExportJson() }
          )
        }

        NavigationTab.CONFIG -> {
          BridgeConfigScreen(
            connectionStatus = connectionStatus,
            latencyMs = latencyMs,
            bridgeHost = bridgeHost,
            bridgePort = bridgePort,
            activeServerUrl = activeServerUrl,
            serverDescription = serverDescription,
            lastLogMessage = lastLogMessage,
            serverPresets = viewModel.serverPresets,
            selectedPreset = selectedPreset,
            shieldConfig = shieldConfig,
            isRunningOnEmulator = isRunningOnEmulator,
            deviceLocalIp = deviceLocalIp,
            subnetPrefix = subnetPrefix,
            probeStatusMessage = probeStatusMessage,
            isProbing = isProbing,
            onSetBridgeHost = { viewModel.setBridgeHost(it) },
            onSetBridgePort = { viewModel.setBridgePort(it) },
            onApplyPreset = { viewModel.applyPreset(it) },
            onConnectPreset = { viewModel.connectToPreset(it) },
            onConnectBridge = { viewModel.connectToBridge() },
            onDisconnectBridge = { viewModel.disconnectBridge() },
            onEnableSimulation = { viewModel.enableSimulationMode() },
            onProbeEndpoint = { host, port -> viewModel.probeEndpoint(host, port) },
            onConnectViaAdbReverse = { port -> viewModel.connectViaAdbReverse(port) },
            onConnectViaLanIp = { ip, port -> viewModel.connectViaLanIp(ip, port) },
            onRefreshNetworkInfo = { viewModel.refreshNetworkInfo() },
            onUpdateRfThresholds = { alert, high -> viewModel.updateRfThresholds(alert, high) },
            onUpdateFsoThresholds = { alert, block -> viewModel.updateFsoThresholds(alert, block) },
            onToggleAutoShield = { viewModel.toggleAutoBlockFso(it) },
            onTriggerLaserAttack = { viewModel.triggerSimulatedLaserAttack() },
            onTriggerRfJammer = { viewModel.triggerSimulatedRogueJammer() },
            onTriggerLidarSpoof = { viewModel.triggerSimulatedLidarSpoofer() },
            isBackgroundServiceRunning = isBackgroundServiceRunning,
            bgRfPacketsFetched = bgRfPacketsFetched,
            bgFsoPacketsFetched = bgFsoPacketsFetched,
            bgLastFetchTimestamp = bgLastFetchTimestamp,
            bgFetchIntervalMs = bgFetchIntervalMs,
            bgServiceLog = bgServiceLog,
            onStartBackgroundService = { viewModel.startBackgroundFetchService(it) },
            onStopBackgroundService = { viewModel.stopBackgroundFetchService() },
            onUpdateBackgroundInterval = { viewModel.setBackgroundFetchInterval(it) },
            onFetchPacketsNow = { viewModel.fetchSignalPacketsNow() }
          )
        }

        NavigationTab.SETTINGS -> {
          SettingsScreen(
            settings = systemSettings,
            onToggleGpsLocation = { viewModel.toggleGpsLocationTracking(it) },
            onToggleEmergencySharing = { viewModel.toggleEmergencyLocationSharing(it) },
            onUpdateFccEnforcement = { enforceNonSim, strictShield, autoReport ->
              viewModel.updateFccEnforcement(enforceNonSim, strictShield, autoReport)
            },
            onToggleHospitalAuth = { hospitalId, granted ->
              viewModel.toggleHospitalViewingAuthorization(hospitalId, granted)
            },
            onToggleHipaaMasking = { viewModel.toggleHipaaMasking(it) },
            onAddEmergencyContact = { viewModel.addEmergencyContact(it) },
            onRemoveEmergencyContact = { viewModel.removeEmergencyContact(it) }
          )
        }
      }
    }
  }
}
