package com.example.network

import android.content.Context
import com.example.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject

/**
 * SignalBridgeClient provides a client facade for WebSocket telemetry streaming,
 * delegating connection management and pipelines to [WebSocketManager].
 */
class SignalBridgeClient(
  private val scope: CoroutineScope,
  val context: Context? = null
) {
  val webSocketManager = WebSocketManager(scope, context)

  val connectionStatus: StateFlow<ConnectionStatus> = webSocketManager.connectionStatus
  val activeServerUrl: StateFlow<String> = webSocketManager.activeServerUrl
  val serverDescription: StateFlow<String> = webSocketManager.serverDescription
  val lastLogMessage: StateFlow<String> = webSocketManager.lastLogMessage
  val latencyMs: StateFlow<Long> = webSocketManager.latencyMs
  val reconnectAttempt: StateFlow<Int> = webSocketManager.reconnectAttempt
  val isReconnecting: StateFlow<Boolean> = webSocketManager.isReconnecting

  val rfFrame: StateFlow<RFSpectrumFrame> = webSocketManager.rfFrame
  val fsoFrame: StateFlow<FSOSpectrumFrame> = webSocketManager.fsoFrame
  val spatialEmitters: StateFlow<List<SpatialEmitter>> = webSocketManager.spatialEmitters
  val modulationClassification: StateFlow<ModulationClassification> = webSocketManager.modulationClassification
  val decodedFrames: StateFlow<List<DecodedFrame>> = webSocketManager.decodedFrames

  val eventFlow: SharedFlow<DetectionEvent> = webSocketManager.detectionEvents
  val telemetryEvents: SharedFlow<TelemetryStreamEvent> = webSocketManager.telemetryEvents

  val isServerScanning: StateFlow<Boolean> = webSocketManager.isServerScanning
  val fsoAllBlocked: StateFlow<Boolean> = webSocketManager.fsoAllBlocked

  fun connect(
    host: String = WebSocketManager.getDefaultBaseUrl(context),
    port: Int = WebSocketManager.DEFAULT_PORT,
    serverName: String = "",
    enableAutoReconnect: Boolean = true
  ) {
    webSocketManager.connect(host, port, serverName, enableAutoReconnect)
  }

  fun disconnect() {
    webSocketManager.disconnect()
  }

  fun switchToSimulation() {
    webSocketManager.switchToSimulation()
  }

  fun sendStartScan() {
    webSocketManager.sendStartScan()
  }

  fun sendStopScan() {
    webSocketManager.sendStopScan()
  }

  fun sendRfThresholds(alertDbm: Float, highDbm: Float) {
    webSocketManager.sendRfThresholds(alertDbm, highDbm)
  }

  fun sendFsoThresholds(alertDbm: Float, blockDbm: Float) {
    webSocketManager.sendFsoThresholds(alertDbm, blockDbm)
  }

  fun sendFsoBlock(band: String) {
    webSocketManager.sendFsoBlock(band)
  }

  fun sendFsoUnblock(band: String) {
    webSocketManager.sendFsoUnblock(band)
  }

  fun sendFsoBlockAll() {
    webSocketManager.sendFsoBlockAll()
  }

  fun sendFsoReset() {
    webSocketManager.sendFsoReset()
  }

  fun sendPing() {
    webSocketManager.sendPing()
  }

  fun sendFetchRfPacket() {
    webSocketManager.sendFetchRfPacket()
  }

  fun sendFetchFsoPacket() {
    webSocketManager.sendFetchFsoPacket()
  }

  fun sendPollTelemetry(pipelines: List<String> = listOf("RF", "FSO")) {
    webSocketManager.sendPollTelemetry(pipelines)
  }

  fun sendExportRequest() {
    webSocketManager.sendExportRequest()
  }

  fun sendJson(json: JSONObject) {
    webSocketManager.sendJson(json)
  }

  fun sendRaw(text: String) {
    webSocketManager.sendRaw(text)
  }

  suspend fun probeTcpPort(host: String, port: Int = 8765): String {
    return webSocketManager.probeTcpPort(host, port)
  }
}
