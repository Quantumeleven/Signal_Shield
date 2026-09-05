package com.example.engine

import com.example.model.*
import kotlin.math.*
import kotlin.random.Random

class SignalSimulationEngine {
  private val random = Random(System.currentTimeMillis())
  private var tick = 0L

  // Shutter state overrides for FSO bands
  private val blockedFsoBands = mutableSetOf<String>()

  // Initial RF Bands
  val initialRfBands = listOf(
    RFBandInfo("Wi-Fi 2.4G Ch 1", "2.4GHz", 2412.0, "802.11b/g/n", "OFDM/DSSS", "Standard 2.4GHz Wi-Fi primary channel"),
    RFBandInfo("Wi-Fi 2.4G Ch 6", "2.4GHz", 2437.0, "802.11ax", "1024-QAM", "High-throughput enterprise AP"),
    RFBandInfo("Wi-Fi 2.4G Ch 11", "2.4GHz", 2462.0, "802.11n", "64-QAM", "IoT device cluster"),
    RFBandInfo("Bluetooth LE Adv", "2.4GHz", 2402.0, "BLE 5.2", "GFSK 2Mbps", "Beacon & peripheral telemetry"),
    RFBandInfo("Zigbee Pro Ch 15", "2.4GHz", 2425.0, "802.15.4", "O-QPSK", "Mesh sensor grid network"),
    RFBandInfo("LTE Band 3 DL", "4G LTE", 1842.5, "3GPP Rel 15", "64-QAM / SC-FDMA", "Cellular base station downlink"),
    RFBandInfo("LTE Band 7 DL", "4G LTE", 2655.0, "3GPP Rel 15", "256-QAM", "High-capacity urban macrocell"),
    RFBandInfo("LTE Band 20 DL", "4G LTE", 806.0, "3GPP Rel 14", "QPSK", "Long-range rural coverage"),
    RFBandInfo("5GHz UNII-1 Ch 36", "5GHz", 5180.0, "802.11ac/ax", "256-QAM", "High-speed 80MHz channel bond"),
    RFBandInfo("5GHz UNII-2C DFS", "5GHz", 5520.0, "802.11ax", "1024-QAM", "Radar-detection capable dynamic link"),
    RFBandInfo("5GHz UNII-3 Ch 149", "5GHz", 5745.0, "802.11be (Wi-Fi 7)", "4096-QAM", "Next-gen ultra-wideband link")
  )

  // Initial FSO Bands
  val initialFsoBands = listOf(
    FSOBandInfo("850nm NIR", 352.7, 850.0, "OOK / PPM", "Short-reach FSO & campus optical interconnect", listOf("Plustek", "MRV", "Cisco")),
    FSOBandInfo("905nm Pulsed NIR", 331.3, 905.0, "PPM / ToF", "Automotive LiDAR & perimeter optical beam", listOf("Velodyne", "LeddarTech", "Luminar")),
    FSOBandInfo("1064nm SWIR", 281.8, 1064.0, "DP-QPSK", "Nd:YAG Tactical & High-Power Optical link", listOf("Coherent", "Thales", "L3Harris")),
    FSOBandInfo("1310nm O-Band", 228.8, 1310.0, "NRZ / PAM4", "Single-mode metropolitan optical carrier", listOf("LightPointe", "fSONA", "Ciena")),
    FSOBandInfo("1550nm C-Band", 193.4, 1550.0, "DWDM DP-16QAM", "Eye-Safe Long-Range Military/Telecom FSO", listOf("Plair", "Mostcom", "Aoptix"))
  )

  // Temporary synthetic threats
  private var forcedThreatType: String? = null
  private var forcedThreatBand: String? = null
  private var forcedThreatDuration = 0

  fun triggerIncursion(type: String, band: String, durationTicks: Int = 30) {
    forcedThreatType = type
    forcedThreatBand = band
    forcedThreatDuration = durationTicks
  }

  fun setFsoBandBlocked(band: String, blocked: Boolean) {
    if (blocked) {
      blockedFsoBands.add(band)
    } else {
      blockedFsoBands.remove(band)
    }
  }

  fun setAllFsoBlocked(blocked: Boolean) {
    if (blocked) {
      blockedFsoBands.addAll(initialFsoBands.map { it.name })
    } else {
      blockedFsoBands.clear()
    }
  }

  fun isFsoBandBlocked(band: String): Boolean = blockedFsoBands.contains(band)

  fun isAllFsoBlocked(): Boolean = blockedFsoBands.size >= initialFsoBands.size

  fun generateRfFrame(config: ShieldConfig): Pair<RFSpectrumFrame, DetectionEvent?> {
    tick++
    var emittedEvent: DetectionEvent? = null

    // Decrement forced threat duration
    if (forcedThreatDuration > 0) {
      forcedThreatDuration--
      if (forcedThreatDuration <= 0) {
        forcedThreatType = null
        forcedThreatBand = null
      }
    }

    val updatedBands = initialRfBands.map { base ->
      var power = -95.0 + sin(tick * 0.1 + base.centerMhz * 0.01) * 6.0 + (random.nextDouble() - 0.5) * 4.0

      // Active channel baseline powers
      when (base.name) {
        "Wi-Fi 2.4G Ch 6" -> power += 28.0 + sin(tick * 0.2) * 5.0
        "Wi-Fi 2.4G Ch 1" -> power += 18.0 + cos(tick * 0.15) * 4.0
        "Bluetooth LE Adv" -> power += 12.0 + if (tick % 6L < 2L) 15.0 else 0.0
        "LTE Band 7 DL" -> power += 22.0 + sin(tick * 0.08) * 3.0
        "5GHz UNII-1 Ch 36" -> power += 25.0 + cos(tick * 0.12) * 6.0
      }

      // Check forced synthetic threat
      if (forcedThreatBand == base.name || (forcedThreatBand == "RF_ALL")) {
        power = -45.0 + random.nextDouble() * 12.0
      }

      val alert = power >= config.rfAlertDbm
      val threat = when {
        power >= config.rfHighDbm -> ThreatLevel.CRITICAL
        power >= config.rfAlertDbm -> ThreatLevel.ELEVATED
        else -> ThreatLevel.NOMINAL
      }

      if (threat == ThreatLevel.CRITICAL && emittedEvent == null && (tick % 15L == 0L || forcedThreatDuration == 29)) {
        emittedEvent = DetectionEvent(
          id = "RF-EVT-${1000 + (tick % 9000)}",
          timestamp = System.currentTimeMillis(),
          source = SignalSource.RF,
          band = base.name,
          powerDbm = round(power * 10) / 10.0,
          thresholdDbm = config.rfHighDbm.toDouble(),
          threatType = forcedThreatType ?: "ROGUE_RF_TRANSMITTER",
          severity = EventSeverity.CRITICAL,
          details = "Signal surge (${round(power * 10) / 10.0} dBm) exceeded high defense threshold on ${base.name} (${base.protocol}).",
          actionTaken = "ALERT_LOGGED_SPECTRAL_LOCK"
        )
      }

      base.copy(
        powerDbm = round(power * 10) / 10.0,
        alert = alert,
        threatLevel = threat
      )
    }

    // Generate PSD curves for RF groups
    val spectraMap = mutableMapOf<String, SpectrumData>()
    
    // 2.4 GHz Spectrum (2400 - 2500 MHz, 64 bins)
    val bins24 = 64
    val freq24 = (0 until bins24).map { 2400.0 + it * (100.0 / bins24) }
    val psd24 = freq24.map { f ->
      var noise = -98.0 + (random.nextDouble() - 0.5) * 3.5
      // Add peaks for Ch1 (2412), Ch6 (2437), Ch11 (2462), BLE (2402)
      noise += gaussianPeak(f, 2412.0, 10.0, 24.0 + sin(tick * 0.15) * 3.0)
      noise += gaussianPeak(f, 2437.0, 12.0, 32.0 + sin(tick * 0.2) * 5.0)
      noise += gaussianPeak(f, 2462.0, 10.0, 15.0 + cos(tick * 0.1) * 3.0)
      noise += gaussianPeak(f, 2402.0, 2.0, if (tick % 6L < 2L) 20.0 else 4.0)
      if (forcedThreatBand == "Wi-Fi 2.4G Ch 6" || forcedThreatBand == "RF_ALL") {
        noise += gaussianPeak(f, 2437.0, 25.0, 45.0)
      }
      round(noise * 10) / 10.0
    }
    spectraMap["2.4GHz"] = SpectrumData(psd24, freq24, "2.4 GHz ISM Spectrum", 2450.0, "HackRF One / RTL-SDR")

    // 4G LTE Spectrum (1800 - 2700 MHz, 64 bins)
    val binsLte = 64
    val freqLte = (0 until binsLte).map { 1800.0 + it * (900.0 / binsLte) }
    val psdLte = freqLte.map { f ->
      var noise = -100.0 + (random.nextDouble() - 0.5) * 3.0
      noise += gaussianPeak(f, 1842.5, 15.0, 22.0)
      noise += gaussianPeak(f, 2655.0, 20.0, 28.0)
      round(noise * 10) / 10.0
    }
    spectraMap["4G LTE"] = SpectrumData(psdLte, freqLte, "LTE Broadband Downlink", 2250.0, "Airspy R2 / SDRplay")

    // 5 GHz Spectrum (5150 - 5850 MHz, 64 bins)
    val bins5 = 64
    val freq5 = (0 until bins5).map { 5150.0 + it * (700.0 / bins5) }
    val psd5 = freq5.map { f ->
      var noise = -102.0 + (random.nextDouble() - 0.5) * 4.0
      noise += gaussianPeak(f, 5180.0, 40.0, 30.0 + sin(tick * 0.1) * 4.0)
      noise += gaussianPeak(f, 5745.0, 30.0, 26.0 + cos(tick * 0.14) * 5.0)
      round(noise * 10) / 10.0
    }
    spectraMap["5GHz"] = SpectrumData(psd5, freq5, "5 GHz UNII Spectrum", 5500.0, "BladeRF 2.0 micro")

    val peakBandObj = updatedBands.maxByOrNull { it.powerDbm }
    val peakBandName = peakBandObj?.name ?: "—"
    val peakPower = peakBandObj?.powerDbm ?: -100.0

    val frame = RFSpectrumFrame(
      timestamp = System.currentTimeMillis(),
      bands = updatedBands,
      spectra = spectraMap,
      peakBand = peakBandName,
      peakPowerDbm = peakPower
    )

    return Pair(frame, emittedEvent)
  }

  fun generateFsoFrame(config: ShieldConfig): Pair<FSOSpectrumFrame, DetectionEvent?> {
    var emittedEvent: DetectionEvent? = null
    val updatedBands = initialFsoBands.map { base ->
      val isBlocked = blockedFsoBands.contains(base.name)
      var power = if (isBlocked) {
        -75.0 + (random.nextDouble() - 0.5) * 2.0 // attenuated down by shutter
      } else {
        -55.0 + sin(tick * 0.12 + base.wavelengthNm * 0.05) * 8.0 + (random.nextDouble() - 0.5) * 3.0
      }

      // Normal baseline powers
      when (base.name) {
        "1550nm C-Band" -> if (!isBlocked) power += 22.0 + sin(tick * 0.15) * 4.0
        "850nm NIR" -> if (!isBlocked) power += 16.0 + cos(tick * 0.2) * 3.0
        "1310nm O-Band" -> if (!isBlocked) power += 18.0 + sin(tick * 0.18) * 3.5
      }

      // Forced threat injection
      if (forcedThreatBand == base.name || (forcedThreatBand == "FSO_ALL")) {
        if (!isBlocked) {
          power = -14.0 + random.nextDouble() * 6.0
        }
      }

      val alert = power >= config.fsoAlertDbm
      val threat = when {
        power >= config.fsoBlockDbm -> ThreatLevel.CRITICAL
        power >= config.fsoAlertDbm -> ThreatLevel.ELEVATED
        else -> ThreatLevel.NOMINAL
      }

      // Automatic shutter actuation if configured
      var currentShutter = if (isBlocked) ShutterState.BLOCKED else ShutterState.OPEN
      if (config.autoBlockFso && power >= config.fsoBlockDbm && !isBlocked) {
        blockedFsoBands.add(base.name)
        currentShutter = ShutterState.SHIELDED
      }

      if (threat == ThreatLevel.CRITICAL && emittedEvent == null && (tick % 12L == 0L || forcedThreatDuration == 29)) {
        emittedEvent = DetectionEvent(
          id = "FSO-EVT-${2000 + (tick % 8000)}",
          timestamp = System.currentTimeMillis(),
          source = SignalSource.FSO,
          band = base.name,
          powerDbm = round(power * 10) / 10.0,
          thresholdDbm = config.fsoBlockDbm.toDouble(),
          threatType = forcedThreatType ?: "OPTICAL_LASER_SURGE",
          severity = EventSeverity.CRITICAL,
          details = "High-energy optical flux (${round(power * 10) / 10.0} dBm) detected on ${base.wavelengthNm} nm. Photodiode saturation danger.",
          actionTaken = if (config.autoBlockFso) "OPTICAL_SHUTTER_ACTIVATED" else "WARNING_FLAGGED"
        )
      }

      val snr = if (isBlocked) 2.0 else max(12.0, 32.0 + power * 0.3)
      val bitrate = if (isBlocked) 0.0 else when (base.name) {
        "1550nm C-Band" -> 10.0
        "1310nm O-Band" -> 2.5
        "850nm NIR" -> 1.25
        else -> 0.622
      }

      base.copy(
        powerDbm = round(power * 10) / 10.0,
        alert = alert,
        blocked = isBlocked || currentShutter == ShutterState.SHIELDED,
        shutterState = currentShutter,
        bitrateGbps = bitrate,
        snrDb = round(snr * 10) / 10.0,
        threatLevel = threat
      )
    }

    // Wavelength spectrum (800nm - 1650nm, 32 bins)
    val wavelengths = listOf(800.0, 830.0, 850.0, 880.0, 905.0, 950.0, 1000.0, 1064.0, 1150.0, 1200.0, 1280.0, 1310.0, 1360.0, 1450.0, 1500.0, 1530.0, 1550.0, 1580.0, 1600.0, 1650.0)
    val psdFso = wavelengths.map { wl ->
      var baseline = -70.0 + (random.nextDouble() - 0.5) * 3.0
      updatedBands.forEach { b ->
        val dist = abs(wl - b.wavelengthNm)
        if (dist < 40.0) {
          val contribution = (b.powerDbm + 70.0) * exp(-dist * dist / 300.0)
          baseline += contribution
        }
      }
      round(baseline * 10) / 10.0
    }

    val frame = FSOSpectrumFrame(
      timestamp = System.currentTimeMillis(),
      bands = updatedBands,
      psd = psdFso,
      wavelengthsNm = wavelengths,
      overallAttenuationDb = round((2.8 + sin(tick * 0.05) * 1.5 + (random.nextDouble() - 0.5) * 0.4) * 10) / 10.0,
      allBlocked = isAllFsoBlocked(),
      ambientLux = round((280.0 + sin(tick * 0.02) * 50.0 + random.nextDouble() * 20.0) * 10) / 10.0
    )

    return Pair(frame, emittedEvent)
  }

  fun generateSpatialEmitters(): List<SpatialEmitter> {
    val isAttack = forcedThreatType != null
    return listOf(
      SpatialEmitter(
        id = "EM-01",
        label = if (isAttack) "ROGUE JAMMER (Hostile)" else "Wi-Fi 6 AP (Enterprise)",
        source = SignalSource.RF,
        azimuthDeg = ((38.0 + sin(tick * 0.03) * 4.0 + (if (isAttack) 104.0 else 0.0)) % 360.0).toFloat(),
        elevationDeg = 4.2f,
        uncertaintyDeg = if (isAttack) 2.2f else 3.8f,
        powerDbm = if (isAttack) -42.0 else -64.0,
        frequencyOrWavelength = "2437 MHz (Ch 6)",
        distanceEstMeters = if (isAttack) 8.5 else 18.2,
        isHostile = isAttack,
        method = "MUSIC 5-Ch Array (KrakenSDR)"
      ),
      SpatialEmitter(
        id = "EM-02",
        label = "LTE Macro Tower DL",
        source = SignalSource.RF,
        azimuthDeg = ((265.0 + cos(tick * 0.02) * 2.0) % 360.0).toFloat(),
        elevationDeg = 1.1f,
        uncertaintyDeg = 4.5f,
        powerDbm = -72.0,
        frequencyOrWavelength = "2655 MHz (Band 7)",
        distanceEstMeters = 24.0,
        isHostile = false,
        method = "ESPRIT Subspace (arXiv:2104.04751)"
      ),
      SpatialEmitter(
        id = "EM-03",
        label = "BLE Sensor Beacon Cluster",
        source = SignalSource.RF,
        azimuthDeg = ((142.0 + sin(tick * 0.05) * 5.0) % 360.0).toFloat(),
        elevationDeg = -2.0f,
        uncertaintyDeg = 6.0f,
        powerDbm = -81.0,
        frequencyOrWavelength = "2402 MHz (Adv Ch 37)",
        distanceEstMeters = 5.4,
        isHostile = false,
        method = "Synthetic Aperture SAR (MIT CSAIL)"
      ),
      SpatialEmitter(
        id = "EM-04",
        label = if (isAttack) "HIGH-POWER LASER SURGE" else "FSO Optical Transceiver",
        source = SignalSource.FSO,
        azimuthDeg = ((312.0 + sin(tick * 0.04) * 2.0) % 360.0).toFloat(),
        elevationDeg = 8.5f,
        uncertaintyDeg = 1.2f,
        powerDbm = if (isAttack) -12.0 else -48.0,
        frequencyOrWavelength = "1550 nm C-Band",
        distanceEstMeters = 12.0,
        isHostile = isAttack,
        method = "Quadrant Photodiode Spatial Error"
      )
    )
  }

  fun generateOpticalAoATarget(): OpticalAoATarget {
    val isAttack = forcedThreatType != null
    val targetX = if (isAttack) 0.55f + sin(tick * 0.15).toFloat() * 0.1f else sin(tick * 0.08).toFloat() * 0.25f
    val targetY = if (isAttack) -0.42f + cos(tick * 0.15).toFloat() * 0.1f else cos(tick * 0.06).toFloat() * 0.20f

    return OpticalAoATarget(
      deltaX = targetX,
      deltaY = targetY,
      intensityWatts = if (isAttack) 0.045 else 0.0018,
      spotSizeMm = if (isAttack) 4.2f else 2.1f,
      wavelengthNm = if (isAttack) 1550.0 else 1310.0,
      lockedOn = true
    )
  }

  fun generateModulationClassification(): Pair<ModulationClassification, List<Pair<String, Float>>> {
    val isAttack = forcedThreatType != null
    val topMod = when {
      forcedThreatType == "BROADBAND_RF_JAMMER" -> "Chirp / Continuous Wave Jammer"
      forcedThreatType == "PULSED_LIDAR_SPOOFER" -> "PPM (Pulse Position Mod)"
      forcedThreatType == "HIGH_POWER_PULSED_LASER" -> "High-Flux DP-16QAM"
      tick % 40L < 20L -> "1024-QAM (802.11ax)"
      else -> "OFDM 64-QAM"
    }

    val confidence = if (isAttack) 0.96f else 0.91f + (random.nextFloat() - 0.5f) * 0.06f

    val candidateList = if (isAttack) {
      listOf(
        topMod to 0.96f,
        "OFDM (Wi-Fi 6)" to 0.02f,
        "GFSK (Bluetooth)" to 0.01f,
        "O-QPSK (Zigbee)" to 0.01f
      )
    } else {
      listOf(
        "1024-QAM (802.11ax)" to 0.88f,
        "64-QAM (802.11n)" to 0.06f,
        "QPSK (3GPP LTE)" to 0.04f,
        "GFSK (BLE 5.2)" to 0.02f
      )
    }

    val classification = ModulationClassification(
      modulation = topMod,
      confidence = confidence.coerceIn(0.70f, 0.99f),
      snrEstDb = if (isAttack) 34.5 else 22.8,
      baudRateEstKhz = if (isAttack) 160000.0 else 80000.0,
      cyclostationaryPeakAlpha = if (isAttack) 20.0 else 40.0
    )

    return Pair(classification, candidateList)
  }

  fun generateDecodedFrames(): List<DecodedFrame> {
    val now = System.currentTimeMillis()
    return listOf(
      DecodedFrame(
        id = "FRM-${1000 + (tick % 500)}",
        timestamp = now - 200,
        protocol = FrameProtocolType.WIFI_802_11,
        channelOrFreq = "2.4 GHz Ch 6 (2437 MHz)",
        sourceAddress = "70:3A:CB:44:91:FA",
        destinationAddress = "FF:FF:FF:FF:FF:FF",
        rssiDbm = -54.0,
        snrDb = 28.0,
        summary = "802.11ax Beacon: SSID='Enterprise_Secure_Net', Cap=0x1431",
        payloadHex = "80 00 00 00 ff ff ff ff ff ff 70 3a cb 44 91 fa 70 3a cb 44 91 fa 30 52 e0 74 12 00 00 00 64 00 31 14 00 15 45 6e 74 65 72 70 72 69 73 65 5f 53 65 63 75 72 65",
        extraInfo = mapOf("BSSID" to "70:3A:CB:44:91:FA", "Beacon Interval" to "100 TU", "Security" to "WPA3-Enterprise SAE", "Channel Width" to "40 MHz")
      ),
      DecodedFrame(
        id = "FRM-${999 + (tick % 500)}",
        timestamp = now - 850,
        protocol = FrameProtocolType.BLE_ADV,
        channelOrFreq = "Adv Ch 37 (2402 MHz)",
        sourceAddress = "C4:D9:87:12:3E:AA",
        destinationAddress = "FF:FF:FF:FF:FF:FF",
        rssiDbm = -68.0,
        snrDb = 19.0,
        summary = "BLE ADV_IND: Apple iBeacon / Sensor Telemetry (UUID=e2c56db5)",
        payloadHex = "02 01 06 1a ff 4c 00 02 15 e2 c5 6d b5 df fb 48 d2 b0 60 d0 f5 a7 10 96 e0 00 01 00 02 c5",
        extraInfo = mapOf("Company ID" to "0x004C (Apple Inc)", "Tx Power" to "-59 dBm", "Major" to "1", "Minor" to "2")
      ),
      DecodedFrame(
        id = "FRM-${998 + (tick % 500)}",
        timestamp = now - 1400,
        protocol = FrameProtocolType.CELLULAR_LTE_SIB,
        channelOrFreq = "LTE Band 7 (2655 MHz)",
        sourceAddress = "eNodeB ID: 418290",
        destinationAddress = "BROADCAST_DL",
        rssiDbm = -75.0,
        snrDb = 14.0,
        summary = "LTE SIB1: PLMN=310-410 (AT&T), TAC=0x4B21, CellID=107082241",
        payloadHex = "40 04 a2 11 c0 08 e2 39 00 1f 83 2a 04 88 00 00 3c",
        extraInfo = mapOf("MCC" to "310 (USA)", "MNC" to "410", "Tracking Area Code" to "19233", "Bandwidth" to "20 MHz")
      ),
      DecodedFrame(
        id = "FRM-${997 + (tick % 500)}",
        timestamp = now - 2100,
        protocol = FrameProtocolType.LORA_CSS,
        channelOrFreq = "915.0 MHz (SF7/BW125)",
        sourceAddress = "DevEUI: A8-40-41-00-01-82",
        destinationAddress = "AppEUI: 70-B3-D5-7E-D0",
        rssiDbm = -92.0,
        snrDb = 9.5,
        summary = "LoRa CSS Uplink: FPort=1, FCnt=1420, Payload=Encrypted Sensor Data",
        payloadHex = "40 01 00 40 a8 80 8c 05 01 3a f1 99 2b 4a 12 cc 88",
        extraInfo = mapOf("Spreading Factor" to "SF7", "Bandwidth" to "125 kHz", "Coding Rate" to "4/5", "FPort" to "1")
      ),
      DecodedFrame(
        id = "FRM-${996 + (tick % 500)}",
        timestamp = now - 2900,
        protocol = FrameProtocolType.OPTICAL_PPM_FSO,
        channelOrFreq = "1550 nm C-Band (193.4 THz)",
        sourceAddress = "OPT-TX-ALPHA-01",
        destinationAddress = "OPT-RX-SHIELD",
        rssiDbm = -38.0,
        snrDb = 26.0,
        summary = "FSO Optical 16-PPM Frame: SyncHeader=0xAA55, Bitrate=10 Gbps",
        payloadHex = "aa 55 aa 55 01 0a 00 80 4f 50 54 49 43 41 4c 5f 50 41 59 4c 4f 41 44 5f 54 45 53 54 5f 44 41 54 41 e4 f1",
        extraInfo = mapOf("Modulation" to "16-PPM / DP-16QAM", "Wavelength" to "1550.0 nm", "BER" to "1.2e-9", "Optical Atten" to "3.1 dB")
      )
    )
  }

  fun generateSigMFMetadata(): Pair<SigMFMetadata, String> {
    val meta = SigMFMetadata(
      version = "1.0.0",
      sampleRateHz = 20_000_000,
      centerFreqHz = 2_437_000_000,
      datatype = "cf32_le",
      hardware = "HackRF One / 5-Ch Antenna Array / Thorlabs TIA QPD",
      author = "Unified Signal Shield v2.4 (arXiv/MIT Additives)",
      description = "Forensic I/Q sample capture with direction finding and demodulation annotations",
      captureTimestampIso = java.time.Instant.now().toString(),
      sampleCount = 131072,
      md5Hash = "a4c28f918e7e112cb90141fbe7890a12"
    )

    val json = """
{
  "global": {
    "core:datatype": "${meta.datatype}",
    "core:sample_rate": ${meta.sampleRateHz},
    "core:version": "${meta.version}",
    "core:hw": "${meta.hardware}",
    "core:author": "${meta.author}",
    "core:description": "${meta.description}",
    "core:sha512": "3a8f192bc932140a87f191024bcda8018241...",
    "core:dataset": "SIG_SHIELD_BURST_${System.currentTimeMillis()}"
  },
  "captures": [
    {
      "core:sample_start": 0,
      "core:frequency": ${meta.centerFreqHz},
      "core:datetime": "${meta.captureTimestampIso}"
    }
  ],
  "annotations": [
    {
      "core:sample_start": 4096,
      "core:sample_count": 65536,
      "core:freq_lower_edge": 2427000000,
      "core:freq_upper_edge": 2447000000,
      "core:label": "802.11ax OFDM Burst / Rogue Jammer",
      "core:comment": "MUSIC AoA Azimuth=142.0 deg, Confidence=0.96"
    }
  ]
}
    """.trimIndent()

    return Pair(meta, json)
  }

  private fun gaussianPeak(x: Double, center: Double, width: Double, height: Double): Double {
    val diff = x - center
    return height * exp(-diff * diff / (2.0 * width * width))
  }
}

