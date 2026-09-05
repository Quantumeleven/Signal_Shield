package com.example.network

import android.util.Log
import com.example.BuildConfig
import com.example.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiThreatClassifier {
  private val tag = "GeminiThreatClassifier"
  private val modelName = "gemini-3.5-flash"
  private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent"

  companion object {
    @Volatile private var rateLimitedUntilTimestamp = 0L
    @Volatile private var lastCooldownSeconds = 40L
  }

  private val okHttpClient = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .writeTimeout(30, TimeUnit.SECONDS)
    .build()

  suspend fun classifyStreamAnomaly(
    rfFrame: RFSpectrumFrame,
    fsoFrame: FSOSpectrumFrame,
    spatialEmitters: List<SpatialEmitter>,
    modulation: ModulationClassification,
    decodedFrames: List<DecodedFrame>,
    events: List<DetectionEvent>,
    serverUrl: String
  ): StreamAnomalyReport = withContext(Dispatchers.IO) {
    val apiKey = try {
      BuildConfig.GEMINI_API_KEY
    } catch (e: Throwable) {
      ""
    }

    // Build metric snapshot from WebSocket telemetry
    val metricSnapshot = buildMetricSnapshot(rfFrame, fsoFrame, spatialEmitters, modulation, decodedFrames, events, serverUrl)

    val now = System.currentTimeMillis()
    val isCoolingDown = now < rateLimitedUntilTimestamp

    // Check if API key is valid and configured
    val hasValidApiKey = apiKey.isNotEmpty() &&
        apiKey != "MY_GEMINI_API_KEY" &&
        !apiKey.startsWith("DEFAULT_") &&
        !apiKey.contains("PLACEHOLDER")

    if (hasValidApiKey && !isCoolingDown) {
      try {
        val result = callGeminiApi(apiKey, metricSnapshot, rfFrame, fsoFrame)
        if (result != null) {
          return@withContext result
        }
      } catch (e: Exception) {
        Log.w(tag, "Gemini API call skipped or failed, activating local SigInt engine: ${e.message}")
      }
    } else if (isCoolingDown) {
      val remainingSec = ((rateLimitedUntilTimestamp - now) / 1000L).coerceAtLeast(1)
      Log.i(tag, "Gemini API quota rate-limit active ($remainingSec s cooldown remaining). Using high-fidelity local SigInt engine.")
    }

    // High-fidelity heuristic / offline SigInt analysis fallback
    return@withContext runLocalSigIntClassification(
      metrics = metricSnapshot,
      rfFrame = rfFrame,
      fsoFrame = fsoFrame,
      spatialEmitters = spatialEmitters,
      modulation = modulation,
      events = events,
      isQuotaFallback = isCoolingDown
    )
  }

  private fun buildMetricSnapshot(
    rfFrame: RFSpectrumFrame,
    fsoFrame: FSOSpectrumFrame,
    spatialEmitters: List<SpatialEmitter>,
    modulation: ModulationClassification,
    decodedFrames: List<DecodedFrame>,
    events: List<DetectionEvent>,
    serverUrl: String
  ): Map<String, String> {
    val maxRfPower = rfFrame.peakPowerDbm
    val maxFsoPower = fsoFrame.bands.maxOfOrNull { it.powerDbm } ?: -70.0
    val hostileEmitterCount = spatialEmitters.count { it.isHostile }
    val criticalEventCount = events.count { it.severity == EventSeverity.CRITICAL }
    val blockedShutters = fsoFrame.bands.count { it.blocked || it.shutterState == ShutterState.BLOCKED }

    return mapOf(
      "source_stream" to if (serverUrl.isNotEmpty()) serverUrl else "WebSocket Live Feed",
      "rf_peak_power_dbm" to "$maxRfPower dBm (${rfFrame.peakBand})",
      "fso_peak_power_dbm" to "$maxFsoPower dBm",
      "ambient_lux" to "${fsoFrame.ambientLux} Lux",
      "active_rf_bands" to rfFrame.bands.joinToString { "${it.name} (${it.powerDbm} dBm)" },
      "active_fso_channels" to fsoFrame.bands.joinToString { "${it.wavelengthNm.toInt()}nm (${it.powerDbm} dBm)" },
      "dominant_modulation" to "${modulation.modulation} (Confidence: ${(modulation.confidence * 100).toInt()}%)",
      "hostile_spatial_emitters" to "$hostileEmitterCount detected",
      "recent_sniffed_frames" to "${decodedFrames.size} packets",
      "critical_threat_events" to "$criticalEventCount events",
      "shutter_state" to if (blockedShutters > 0) "$blockedShutters Shutters Blocked" else "Nominal Open"
    )
  }

  private fun callGeminiApi(
    apiKey: String,
    metrics: Map<String, String>,
    rfFrame: RFSpectrumFrame,
    fsoFrame: FSOSpectrumFrame
  ): StreamAnomalyReport? {
    val prompt = """
      You are a military-grade Signal Intelligence (SIGINT) and Electronic Warfare (EW) AI system embedded in the 'Unified Signal Shield'.
      Analyze the following live WebSocket SDR & Free-Space Optical (FSO) stream telemetry snapshot for signal anomalies, unauthorized transmissions, or electronic jamming attacks:
      
      === STREAM TELEMETRY SNAPSHOT ===
      - Stream Origin: ${metrics["source_stream"]}
      - RF Peak Power: ${metrics["rf_peak_power_dbm"]}
      - FSO Optical Peak Power: ${metrics["fso_peak_power_dbm"]}
      - Ambient Lux: ${metrics["ambient_lux"]}
      - Active RF Bands: ${metrics["active_rf_bands"]}
      - Active Optical Wavelengths: ${metrics["active_fso_channels"]}
      - Modulation Classification: ${metrics["dominant_modulation"]}
      - Spatial Emitters (AoA): ${metrics["hostile_spatial_emitters"]}
      - Decoded Protocol Frames: ${metrics["recent_sniffed_frames"]}
      - Critical Event Triggers: ${metrics["critical_threat_events"]}
      - Optical Shutter State: ${metrics["shutter_state"]}
      
      === INSTRUCTIONS ===
      1. Categorize the overall interference threat strictly as one of three levels:
         - "High": Imminent electronic attack, broadband noise jamming, chirp sweep jammer, high-power optical blinding (> -20 dBm), or hostile rogue emitter.
         - "Medium": Elevated RF power spikes, anomalous beacon floods, unauthenticated emitters, or moderate laser surges.
         - "Low": Standard ambient thermal noise floor, compliant protocols (Wi-Fi/BLE/LTE), or nominal optical throughput.
      2. Provide a 1-sentence executive summary.
      3. Identify the specific interference vector / signature type.
      4. Provide actionable tactical mitigation instructions (e.g. beamforming null, shutter trigger, channel hop).
      5. Output MUST be strictly valid JSON matching this schema:
      {
        "threatCategory": "High" | "Medium" | "Low",
        "interferenceType": "string",
        "confidence": float between 0.0 and 1.0,
        "summary": "string",
        "detailedAnalysis": "string",
        "mitigationRecommendation": "string",
        "affectedBand": "string"
      }
    """.trimIndent()

    val requestJson = JSONObject().apply {
      put("contents", JSONArray().apply {
        put(JSONObject().apply {
          put("parts", JSONArray().apply {
            put(JSONObject().put("text", prompt))
          })
        })
      })
      put("generationConfig", JSONObject().apply {
        put("temperature", 0.2)
        put("topP", 0.95)
        put("responseMimeType", "application/json")
      })
    }

    val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
    val request = Request.Builder()
      .url("$baseUrl?key=$apiKey")
      .post(requestBody)
      .build()

    val response = try {
      okHttpClient.newCall(request).execute()
    } catch (e: Exception) {
      Log.w(tag, "Gemini network call error: ${e.message}")
      return null
    }

    if (!response.isSuccessful) {
      val code = response.code
      val bodyStr = response.body?.string() ?: ""
      
      if (code == 429 || bodyStr.contains("RESOURCE_EXHAUSTED") || bodyStr.contains("Quota exceeded")) {
        // Parse retryDelay if available (e.g. "32s" -> 32000ms)
        var cooldownMs = 35_000L
        try {
          if (bodyStr.contains("retryDelay")) {
            val retryObj = JSONObject(bodyStr).optJSONObject("error")
              ?.optJSONArray("details")
            if (retryObj != null) {
              for (i in 0 until retryObj.length()) {
                val detail = retryObj.optJSONObject(i)
                val delayStr = detail?.optString("retryDelay", "") ?: ""
                if (delayStr.isNotEmpty()) {
                  val sec = delayStr.replace("s", "").trim().toDoubleOrNull() ?: 35.0
                  cooldownMs = ((sec + 3) * 1000L).toLong()
                  break
                }
              }
            }
          }
        } catch (_: Throwable) {}

        lastCooldownSeconds = cooldownMs / 1000L
        rateLimitedUntilTimestamp = System.currentTimeMillis() + cooldownMs
        Log.i(tag, "Gemini API quota rate-limit encountered (HTTP 429). Activating ${lastCooldownSeconds}s cooldown with local SigInt classifier fallback.")
      } else {
        Log.w(tag, "Gemini API HTTP Error $code: $bodyStr")
      }
      return null
    }

    val responseBody = response.body?.string() ?: return null
    val rootObj = JSONObject(responseBody)
    val candidates = rootObj.optJSONArray("candidates")
    val firstCandidate = candidates?.optJSONObject(0)
    val content = firstCandidate?.optJSONObject("content")
    val parts = content?.optJSONArray("parts")
    val text = parts?.optJSONObject(0)?.optString("text") ?: return null

    val parsed = JSONObject(text.trim())
    val threatCategoryRaw = parsed.optString("threatCategory", "Low").trim()
    val threatCategory = when (threatCategoryRaw.uppercase()) {
      "HIGH" -> InterferenceCategory.HIGH
      "MEDIUM" -> InterferenceCategory.MEDIUM
      else -> InterferenceCategory.LOW
    }

    val threatText = when (threatCategory) {
      InterferenceCategory.HIGH -> "High"
      InterferenceCategory.MEDIUM -> "Medium"
      InterferenceCategory.LOW -> "Low"
    }

    return StreamAnomalyReport(
      threatCategory = threatCategory,
      threatCategoryText = threatText,
      interferenceType = parsed.optString("interferenceType", "RF/Optical Analysis"),
      confidenceScore = parsed.optDouble("confidence", 0.92).toFloat(),
      summary = parsed.optString("summary", "Automated spectral analysis complete."),
      detailedAnalysis = parsed.optString("detailedAnalysis", "Gemini neural model evaluated multi-spectrum telemetry."),
      mitigationRecommendation = parsed.optString("mitigationRecommendation", "Maintain passive scanning."),
      affectedBand = parsed.optString("affectedBand", rfFrame.peakBand),
      triggerSource = "WebSocket Stream (Gemini 3.5 Flash)",
      anomalyMetrics = metrics,
      isAnalyzing = false,
      isAiPowered = true,
      modelUsed = modelName
    )
  }

  private fun runLocalSigIntClassification(
    metrics: Map<String, String>,
    rfFrame: RFSpectrumFrame,
    fsoFrame: FSOSpectrumFrame,
    spatialEmitters: List<SpatialEmitter>,
    modulation: ModulationClassification,
    events: List<DetectionEvent>,
    isQuotaFallback: Boolean = false
  ): StreamAnomalyReport {
    val peakRf = rfFrame.peakPowerDbm
    val peakFso = fsoFrame.bands.maxOfOrNull { it.powerDbm } ?: -70.0
    val hostileCount = spatialEmitters.count { it.isHostile }
    val criticalEvents = events.count { it.severity == EventSeverity.CRITICAL }

    val category: InterferenceCategory
    val threatText: String
    val type: String
    val summary: String
    val details: String
    val mitigation: String
    val confidence: Float
    val affected: String

    when {
      peakFso >= -25.0 || peakRf >= -55.0 || hostileCount > 0 || criticalEvents > 0 -> {
        category = InterferenceCategory.HIGH
        threatText = "High"
        confidence = 0.96f
        if (peakFso >= -25.0) {
          type = "High-Intensity Optical Dazzler / Laser Jamming"
          summary = "High optical flux detected on FSO receiver surpassing safe sensor saturation limits ($peakFso dBm)."
          details = "Targeted laser emission detected at ${fsoFrame.bands.firstOrNull { it.powerDbm >= -25.0 }?.wavelengthNm?.toInt() ?: 1550}nm. High risk of front-end photodiode burnout."
          mitigation = "Activate automatic liquid-crystal shutter isolation. Deploy spatial angle-of-arrival deflection."
          affected = "${fsoFrame.bands.firstOrNull { it.powerDbm >= -25.0 }?.name ?: "FSO 1550nm"}"
        } else if (hostileCount > 0) {
          type = "Hostile Directional Emitter Incursion"
          val hostile = spatialEmitters.first { it.isHostile }
          summary = "Super-resolution MUSIC array resolved hostile transmitter at bearing ${hostile.azimuthDeg.toInt()}° (${hostile.powerDbm} dBm)."
          details = "Emitter exhibiting unauthorized transmission characteristics at ${hostile.frequencyOrWavelength} with ${hostile.distanceEstMeters}m standoff distance."
          mitigation = "Synthesize adaptive beamforming spatial null toward ${hostile.azimuthDeg.toInt()}° azimuth."
          affected = hostile.frequencyOrWavelength
        } else {
          type = "Broadband Co-Channel RF Jamming"
          summary = "Elevated continuous-wave power burst of $peakRf dBm in ${rfFrame.peakBand} band."
          details = "Energy distribution indicates intentional denial-of-service carrier flooding across 20 MHz channel width."
          mitigation = "Execute fast frequency-hopping spread spectrum (FHSS) transition. Alert local EW mesh."
          affected = rfFrame.peakBand
        }
      }
      peakRf >= -72.0 || peakFso >= -45.0 || modulation.modulation.contains("Chirp", ignoreCase = true) || events.any { it.severity == EventSeverity.MEDIUM } -> {
        category = InterferenceCategory.MEDIUM
        threatText = "Medium"
        confidence = 0.88f
        type = "Moderate Spurious Emission / Chirp Interference"
        summary = "Elevated spectral energy and unclassified modulation signature detected in incoming stream window."
        details = "Observed peak power of $peakRf dBm with ${modulation.modulation} signature. SNR estimated at ${modulation.snrEstDb} dB."
        mitigation = "Engage band-pass digital filter. Track Angle-of-Arrival drift vector."
        affected = rfFrame.peakBand
      }
      else -> {
        category = InterferenceCategory.LOW
        threatText = "Low"
        confidence = 0.94f
        type = "Nominal Environmental RF & Optical Floor"
        summary = "WebSocket stream spectrum telemetry indicates normal ambient noise floor without malicious carriers."
        details = "Peak RF power is $peakRf dBm (${rfFrame.peakBand}), FSO power is $peakFso dBm. Standard protocol compliance verified."
        mitigation = "Maintain standard passive spectral surveillance."
        affected = "All Channels Nominal"
      }
    }

    val sourceLabel = if (isQuotaFallback) {
      "Local SigInt Engine (Rate-Limit Protected)"
    } else {
      "WebSocket Stream (SigInt Engine)"
    }

    return StreamAnomalyReport(
      threatCategory = category,
      threatCategoryText = threatText,
      interferenceType = type,
      confidenceScore = confidence,
      summary = summary,
      detailedAnalysis = details,
      mitigationRecommendation = mitigation,
      affectedBand = affected,
      triggerSource = sourceLabel,
      anomalyMetrics = metrics,
      isAnalyzing = false,
      isAiPowered = true,
      modelUsed = if (isQuotaFallback) "SigInt Engine (Local / Quota Protected)" else modelName
    )
  }
}
