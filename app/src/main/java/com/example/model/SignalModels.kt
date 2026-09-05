package com.example.model

enum class SignalSource {
  RF, FSO
}

enum class ThreatLevel {
  NOMINAL, ELEVATED, CRITICAL
}

enum class EventSeverity {
  INFO, MEDIUM, HIGH, CRITICAL
}

enum class ShutterState {
  OPEN, BLOCKED, SHIELDED
}

enum class ConnectionStatus {
  CONNECTED, CONNECTING, DISCONNECTED, SIMULATION
}

data class RFBandInfo(
  val name: String,
  val group: String,
  val centerMhz: Double,
  val protocol: String,
  val modulation: String,
  val description: String = "",
  val powerDbm: Double = -95.0,
  val alert: Boolean = false,
  val threatLevel: ThreatLevel = ThreatLevel.NOMINAL
)

data class SpectrumData(
  val psd: List<Double>,
  val freqMhz: List<Double>,
  val label: String = "",
  val centerMhz: Double = 0.0,
  val hwNote: String = ""
)

data class RFSpectrumFrame(
  val timestamp: Long = System.currentTimeMillis(),
  val bands: List<RFBandInfo> = emptyList(),
  val spectra: Map<String, SpectrumData> = emptyMap(),
  val peakBand: String = "—",
  val peakPowerDbm: Double = -100.0
)

data class FSOBandInfo(
  val name: String,
  val freqThz: Double,
  val wavelengthNm: Double,
  val modulation: String,
  val description: String = "",
  val vendors: List<String> = emptyList(),
  val powerDbm: Double = -65.0,
  val alert: Boolean = false,
  val blocked: Boolean = false,
  val shutterState: ShutterState = ShutterState.OPEN,
  val bitrateGbps: Double = 1.25,
  val snrDb: Double = 24.0,
  val threatLevel: ThreatLevel = ThreatLevel.NOMINAL
)

data class FSOSpectrumFrame(
  val timestamp: Long = System.currentTimeMillis(),
  val bands: List<FSOBandInfo> = emptyList(),
  val psd: List<Double> = emptyList(),
  val wavelengthsNm: List<Double> = emptyList(),
  val overallAttenuationDb: Double = 3.2,
  val allBlocked: Boolean = false,
  val ambientLux: Double = 320.0
)

data class DetectionEvent(
  val id: String,
  val timestamp: Long,
  val source: SignalSource,
  val band: String,
  val powerDbm: Double,
  val thresholdDbm: Double,
  val threatType: String,
  val severity: EventSeverity,
  val details: String,
  val actionTaken: String
)

data class ShieldConfig(
  val rfAlertDbm: Float = -80f,
  val rfHighDbm: Float = -60f,
  val fsoAlertDbm: Float = -40f,
  val fsoBlockDbm: Float = -20f,
  val autoBlockFso: Boolean = true,
  val soundAlarm: Boolean = true,
  val waterfallSpeed: Float = 1.0f
)

data class WaterfallRow(
  val timestamp: Long,
  val powers: FloatArray
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is WaterfallRow) return false
    return timestamp == other.timestamp && powers.contentEquals(other.powers)
  }

  override fun hashCode(): Int {
    var result = timestamp.hashCode()
    result = 31 * result + powers.contentHashCode()
    return result
  }
}

// ---- arXiv / MIT Open Source Additions ----

enum class FrameProtocolType {
  WIFI_802_11, BLE_ADV, CELLULAR_LTE_SIB, LORA_CSS, OPTICAL_PPM_FSO
}

data class DecodedFrame(
  val id: String,
  val timestamp: Long = System.currentTimeMillis(),
  val protocol: FrameProtocolType,
  val channelOrFreq: String,
  val sourceAddress: String, // MAC, UUID, or Optical Stream ID
  val destinationAddress: String = "FF:FF:FF:FF:FF:FF",
  val rssiDbm: Double,
  val snrDb: Double,
  val summary: String,
  val payloadHex: String,
  val extraInfo: Map<String, String> = emptyMap()
)

data class ModulationClassification(
  val modulation: String,
  val confidence: Float,
  val snrEstDb: Double,
  val baudRateEstKhz: Double,
  val algorithm: String = "RadioML ResNet-1D / CNN (arXiv:1602.04105)",
  val cyclostationaryPeakAlpha: Double = 0.0
)

data class SpatialEmitter(
  val id: String,
  val label: String,
  val source: SignalSource,
  val azimuthDeg: Float,      // 0 - 360 degrees
  val elevationDeg: Float = 0f, // -90 to +90
  val uncertaintyDeg: Float = 4.5f,
  val powerDbm: Double,
  val frequencyOrWavelength: String,
  val distanceEstMeters: Double = 15.0,
  val isHostile: Boolean = false,
  val method: String = "MUSIC Super-Resolution (KrakenSDR / Lincoln Lab)"
)

data class OpticalAoATarget(
  val deltaX: Float, // Normalized -1.0 to 1.0 from Quadrant Photodiode
  val deltaY: Float, // Normalized -1.0 to 1.0 from Quadrant Photodiode
  val intensityWatts: Double,
  val spotSizeMm: Float,
  val wavelengthNm: Double,
  val lockedOn: Boolean
)

data class SigMFMetadata(
  val version: String = "1.0.0",
  val sampleRateHz: Long = 20_000_000,
  val centerFreqHz: Long = 2_437_000_000,
  val datatype: String = "cf32_le",
  val hardware: String = "RTL-SDR / HackRF One / Thorlabs TIA QPD",
  val author: String = "Unified Signal Shield v2.4",
  val description: String = "Captured Anomaly Burst (RF/FSO)",
  val captureTimestampIso: String = "",
  val sampleCount: Long = 131072,
  val md5Hash: String = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
)

data class ServerPreset(
  val id: String,
  val title: String,
  val institution: String,
  val url: String,
  val defaultPort: Int,
  val description: String,
  val isSecure: Boolean = true,
  val tag: String = "OPEN"
)

enum class InterferenceCategory {
  HIGH, MEDIUM, LOW
}

data class StreamAnomalyReport(
  val id: String = java.util.UUID.randomUUID().toString(),
  val timestamp: Long = System.currentTimeMillis(),
  val threatCategory: InterferenceCategory = InterferenceCategory.LOW,
  val threatCategoryText: String = "Low", // "High", "Medium", or "Low"
  val interferenceType: String = "Nominal Environmental RF Floor",
  val confidenceScore: Float = 0.94f,
  val summary: String = "WebSocket stream spectrum telemetry indicates standard low-power ambient background noise.",
  val detailedAnalysis: String = "No high-power unauthorized CW carriers or laser saturation detected in current stream window.",
  val mitigationRecommendation: String = "Maintain passive spectrum sweep; no directional nulling or optical shutter activation required.",
  val affectedBand: String = "2.4 GHz / 850 nm",

  val triggerSource: String = "WebSocket Stream Monitor",
  val anomalyMetrics: Map<String, String> = emptyMap(),
  val isAnalyzing: Boolean = false,
  val errorMessage: String? = null,
  val isAiPowered: Boolean = true,
  val modelUsed: String = "gemini-3.5-flash"
)



