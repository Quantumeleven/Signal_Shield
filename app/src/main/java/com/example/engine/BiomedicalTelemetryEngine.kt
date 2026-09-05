package com.example.engine

import com.example.model.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.*
import kotlin.random.Random

class BiomedicalTelemetryEngine {
  private val random = Random(System.currentTimeMillis())
  private var tick = 0L

  private val devices = mutableListOf(
    BiomedicalDevice(
      id = "MED-PAC-8041",
      patientName = "Eleanor Vance",
      patientRoom = "Cardiac Care Unit 3B",
      patientAge = 67,
      deviceType = BiomedicalDeviceType.PACEMAKER_IMPLANT,
      rfBandFrequency = "402.5 MHz MedRadio (MICS)",
      hospitalId = "UMC-MAIN",
      hospitalName = "University Medical Center & CCU",
      biomedicalPartner = "Medtronic / Philips Healthcare",
      hospitalAuthorizationGranted = true,
      latitude = 37.7749,
      longitude = -122.4194,
      heartRateBpm = 72,
      spO2Percent = 98,
      bloodPressureSys = 124,
      bloodPressureDia = 82,
      glucoseMgDl = 98,
      respirationRateBpm = 15,
      batteryPercent = 89,
      rssiDbm = -62.0,
      packetLossPercent = 0.2f,
      status = BiomedicalDeviceStatus.NOMINAL,
      ecgSamples = generateEcgWaveform(72, false),
      vitalsHistory = listOf(71, 72, 70, 72, 73, 72, 71, 72, 72, 74)
    ),
    BiomedicalDevice(
      id = "MED-PUMP-209",
      patientName = "Marcus Thorne",
      patientRoom = "ICU Bed 02",
      patientAge = 54,
      deviceType = BiomedicalDeviceType.SMART_INFUSION_PUMP,
      rfBandFrequency = "2.4 GHz Wi-Fi IoT",
      hospitalId = "UMC-MAIN",
      hospitalName = "University Medical Center & CCU",
      biomedicalPartner = "Medtronic / Philips Healthcare",
      hospitalAuthorizationGranted = true,
      latitude = 37.7751,
      longitude = -122.4190,
      heartRateBpm = 86,
      spO2Percent = 95,
      bloodPressureSys = 138,
      bloodPressureDia = 88,
      infusionRateMlH = 15.0f,
      glucoseMgDl = 115,
      respirationRateBpm = 18,
      batteryPercent = 96,
      rssiDbm = -58.0,
      packetLossPercent = 0.0f,
      status = BiomedicalDeviceStatus.NOMINAL,
      ecgSamples = generateEcgWaveform(86, false),
      vitalsHistory = listOf(84, 85, 85, 86, 86, 87, 86, 85, 86, 86)
    ),
    BiomedicalDevice(
      id = "MED-VENT-550",
      patientName = "David Chen",
      patientRoom = "Trauma Bay 1",
      patientAge = 42,
      deviceType = BiomedicalDeviceType.ICU_VENTILATOR,
      rfBandFrequency = "5.8 GHz Medical Telemetry",
      hospitalId = "UMC-MAIN",
      hospitalName = "University Medical Center & CCU",
      biomedicalPartner = "Medtronic / Philips Healthcare",
      hospitalAuthorizationGranted = true,
      latitude = 37.7745,
      longitude = -122.4198,
      heartRateBpm = 110,
      spO2Percent = 91,
      bloodPressureSys = 145,
      bloodPressureDia = 94,
      respirationRateBpm = 24,
      batteryPercent = 74,
      rssiDbm = -68.0,
      packetLossPercent = 1.4f,
      status = BiomedicalDeviceStatus.ABNORMAL_VITALS,
      anomalyDescription = "High Peak Inspiratory Pressure & Tachycardia Detected",
      emergencyContactRecommended = false,
      ecgSamples = generateEcgWaveform(110, false),
      vitalsHistory = listOf(95, 98, 102, 105, 108, 109, 112, 110, 111, 110)
    ),
    BiomedicalDevice(
      id = "MED-ECG-118",
      patientName = "Sarah Jenkins",
      patientRoom = "Step-Down Wing Rm 104",
      patientAge = 71,
      deviceType = BiomedicalDeviceType.ECG_TELEMETRY_MONITOR,
      rfBandFrequency = "433.92 MHz ISM",
      hospitalId = "ST-MARYS-COMMUNITY",
      hospitalName = "St. Mary's Regional Hospital",
      biomedicalPartner = "Baxter / GE Healthcare",
      hospitalAuthorizationGranted = true,
      latitude = 37.7758,
      longitude = -122.4180,
      heartRateBpm = 68,
      spO2Percent = 99,
      bloodPressureSys = 118,
      bloodPressureDia = 76,
      glucoseMgDl = 92,
      respirationRateBpm = 14,
      batteryPercent = 42,
      rssiDbm = -71.0,
      packetLossPercent = 0.5f,
      status = BiomedicalDeviceStatus.NOMINAL,
      ecgSamples = generateEcgWaveform(68, false),
      vitalsHistory = listOf(68, 67, 68, 69, 68, 68, 67, 68, 68, 68)
    ),
    BiomedicalDevice(
      id = "MED-CGM-902",
      patientName = "Amina Al-Mansoor",
      patientRoom = "Outpatient Telemetry #12",
      patientAge = 35,
      deviceType = BiomedicalDeviceType.GLUCOSE_MONITOR_CGM,
      rfBandFrequency = "BLE 2.4G",
      hospitalId = "ST-MARYS-COMMUNITY",
      hospitalName = "St. Mary's Regional Hospital",
      biomedicalPartner = "Baxter / GE Healthcare",
      hospitalAuthorizationGranted = true,
      latitude = 37.7761,
      longitude = -122.4175,
      heartRateBpm = 75,
      spO2Percent = 98,
      bloodPressureSys = 120,
      bloodPressureDia = 80,
      glucoseMgDl = 142,
      respirationRateBpm = 16,
      batteryPercent = 88,
      rssiDbm = -65.0,
      packetLossPercent = 0.1f,
      status = BiomedicalDeviceStatus.NOMINAL,
      ecgSamples = generateEcgWaveform(75, false),
      vitalsHistory = listOf(130, 132, 135, 138, 140, 142, 141, 142, 143, 142)
    ),
    BiomedicalDevice(
      id = "MED-SPO2-334",
      patientName = "Robert Kowalski",
      patientRoom = "Post-Op Recovery Bed 6",
      patientAge = 63,
      deviceType = BiomedicalDeviceType.PULSE_OXIMETER_SPO2,
      rfBandFrequency = "BLE 5.2 Medical",
      hospitalId = "MEMORIAL-CARDIO",
      hospitalName = "Memorial Cardiovascular Institute",
      biomedicalPartner = "Abbott Medical / Boston Scientific",
      hospitalAuthorizationGranted = false, // Not yet verified by hospital BAA
      latitude = 37.7735,
      longitude = -122.4210,
      heartRateBpm = 78,
      spO2Percent = 97,
      bloodPressureSys = 126,
      bloodPressureDia = 84,
      respirationRateBpm = 16,
      batteryPercent = 15,
      rssiDbm = -78.0,
      packetLossPercent = 2.8f,
      status = BiomedicalDeviceStatus.BATTERY_DEPLETED,
      anomalyDescription = "Low Battery (15%) - Transducer Replacement Required",
      emergencyContactRecommended = false,
      ecgSamples = generateEcgWaveform(78, false),
      vitalsHistory = listOf(76, 77, 78, 77, 78, 79, 78, 78, 77, 78)
    )
  )

  fun tickDevices(): List<BiomedicalDevice> {
    tick++
    val updated = devices.mapIndexed { index, device ->
      val jitter = (random.nextFloat() - 0.5f) * 2f

      // Vitals drift slightly unless locked in emergency state
      val hr = when (device.status) {
        BiomedicalDeviceStatus.CRITICAL_FAILURE -> (device.heartRateBpm + (random.nextInt(5) - 2)).coerceIn(140, 220)
        BiomedicalDeviceStatus.RF_INTERFERENCE_JAMMED -> (device.heartRateBpm + (random.nextInt(3) - 1)).coerceIn(40, 160)
        BiomedicalDeviceStatus.NOMINAL -> (device.heartRateBpm + (if (tick % 5 == 0L) (random.nextInt(3) - 1) else 0)).coerceIn(60, 95)
        else -> device.heartRateBpm
      }

      val spo2 = when (device.status) {
        BiomedicalDeviceStatus.CRITICAL_FAILURE -> (device.spO2Percent - if (tick % 3 == 0L) 1 else 0).coerceIn(75, 92)
        BiomedicalDeviceStatus.NOMINAL -> (device.spO2Percent + (if (random.nextFloat() > 0.8f) random.nextInt(2) - 1 else 0)).coerceIn(95, 100)
        else -> device.spO2Percent
      }

      val isVentOrFib = device.status == BiomedicalDeviceStatus.CRITICAL_FAILURE || device.status == BiomedicalDeviceStatus.MALICIOUS_TAMPER_ATTACK
      val samples = generateEcgWaveform(hr, isVentOrFib)

      val history = (device.vitalsHistory.drop(1) + hr)

      val packetLoss = when (device.status) {
        BiomedicalDeviceStatus.RF_INTERFERENCE_JAMMED -> (device.packetLossPercent + 5.0f).coerceAtMost(98.5f)
        BiomedicalDeviceStatus.MALICIOUS_TAMPER_ATTACK -> 42.0f
        else -> (device.packetLossPercent + (random.nextFloat() - 0.5f) * 0.2f).coerceIn(0.0f, 3.5f)
      }

      device.copy(
        heartRateBpm = hr,
        spO2Percent = spo2,
        packetLossPercent = packetLoss,
        lastTelemetryTimestamp = System.currentTimeMillis(),
        ecgSamples = samples,
        vitalsHistory = history
      )
    }

    devices.clear()
    devices.addAll(updated)
    return devices.toList()
  }

  fun injectPacemakerJamming(deviceId: String = "MED-PAC-8041"): BiomedicalDevice? {
    val idx = devices.indexOfFirst { it.id == deviceId }
    if (idx != -1) {
      val d = devices[idx]
      val modified = d.copy(
        status = BiomedicalDeviceStatus.RF_INTERFERENCE_JAMMED,
        anomalyDescription = "402.5 MHz MedRadio RF Carrier Jammed: Severe Telemetry Degradation (95% Packet Drop)",
        rssiDbm = -96.0,
        packetLossPercent = 95.0f,
        heartRateBpm = 48, // Bradycardia pacing loss
        emergencyContactRecommended = true
      )
      devices[idx] = modified
      return modified
    }
    return null
  }

  fun injectArrhythmiaVfib(deviceId: String = "MED-PAC-8041"): BiomedicalDevice? {
    val idx = devices.indexOfFirst { it.id == deviceId }
    if (idx != -1) {
      val d = devices[idx]
      val modified = d.copy(
        status = BiomedicalDeviceStatus.CRITICAL_FAILURE,
        anomalyDescription = "ACUTE VENTRICULAR FIBRILLATION / CARDIAC ARREST DETECTED",
        heartRateBpm = 185,
        spO2Percent = 82,
        bloodPressureSys = 70,
        bloodPressureDia = 40,
        emergencyContactRecommended = true
      )
      devices[idx] = modified
      return modified
    }
    return null
  }

  fun injectInfusionOverdose(deviceId: String = "MED-PUMP-209"): BiomedicalDevice? {
    val idx = devices.indexOfFirst { it.id == deviceId }
    if (idx != -1) {
      val d = devices[idx]
      val modified = d.copy(
        status = BiomedicalDeviceStatus.MALICIOUS_TAMPER_ATTACK,
        anomalyDescription = "UNAUTHORIZED REMOTE RATE OVERRIDE (350 mL/h vs Prescribed 15 mL/h)",
        infusionRateMlH = 350.0f,
        heartRateBpm = 135,
        bloodPressureSys = 165,
        emergencyContactRecommended = true
      )
      devices[idx] = modified
      return modified
    }
    return null
  }

  fun injectVentilatorFailure(deviceId: String = "MED-VENT-550"): BiomedicalDevice? {
    val idx = devices.indexOfFirst { it.id == deviceId }
    if (idx != -1) {
      val d = devices[idx]
      val modified = d.copy(
        status = BiomedicalDeviceStatus.CRITICAL_FAILURE,
        anomalyDescription = "CRITICAL CIRCUIT APNEA & PRESSURE DROP: PATIENT NOT VENTILATING",
        spO2Percent = 78,
        heartRateBpm = 142,
        respirationRateBpm = 6,
        emergencyContactRecommended = true
      )
      devices[idx] = modified
      return modified
    }
    return null
  }

  fun resolveDevice(deviceId: String): BiomedicalDevice? {
    val idx = devices.indexOfFirst { it.id == deviceId }
    if (idx != -1) {
      val d = devices[idx]
      val modified = d.copy(
        status = BiomedicalDeviceStatus.NOMINAL,
        anomalyDescription = null,
        emergencyContactRecommended = false,
        packetLossPercent = 0.2f,
        rssiDbm = -62.0,
        heartRateBpm = 72,
        spO2Percent = 98,
        infusionRateMlH = 15.0f,
        respirationRateBpm = 16
      )
      devices[idx] = modified
      return modified
    }
    return null
  }

  fun restoreAllDevices(): List<BiomedicalDevice> {
    devices.indices.forEach { i ->
      val d = devices[i]
      devices[i] = d.copy(
        status = BiomedicalDeviceStatus.NOMINAL,
        anomalyDescription = null,
        emergencyContactRecommended = false,
        packetLossPercent = 0.2f,
        rssiDbm = -60.0,
        heartRateBpm = 72,
        spO2Percent = 98,
        infusionRateMlH = 15.0f,
        respirationRateBpm = 16
      )
    }
    return devices.toList()
  }

  fun createEmergencyReport(device: BiomedicalDevice): MedicalEmergencyDispatch {
    val vitalsStr = "HR: ${device.heartRateBpm} BPM | SpO2: ${device.spO2Percent}% | BP: ${device.bloodPressureSys}/${device.bloodPressureDia} mmHg | Resp: ${device.respirationRateBpm} BPM"
    val failureReason = device.anomalyDescription ?: "Device Malfunction / Sensor Alert"
    val action = when (device.status) {
      BiomedicalDeviceStatus.CRITICAL_FAILURE -> "Immediate Code Blue / Crash Cart & Advanced Cardiac Life Support (ACLS) required."
      BiomedicalDeviceStatus.RF_INTERFERENCE_JAMMED -> "Switch to hardwired telemetry backup and isolate MedRadio RF band (402-405 MHz)."
      BiomedicalDeviceStatus.MALICIOUS_TAMPER_ATTACK -> "Immediate physical manual power cut to infusion line and manual titration."
      else -> "Urgent bedside medical evaluation required."
    }

    return MedicalEmergencyDispatch(
      patientName = device.patientName,
      location = device.patientRoom,
      deviceId = device.id,
      deviceType = device.deviceType,
      currentVitalsSummary = vitalsStr,
      severity = if (device.status.isEmergency) "CRITICAL CODE BLUE" else "ELEVATED MEDICAL ALERT",
      failureReason = failureReason,
      recommendedMedicalAction = action,
      emergencyPhoneNumber = "911",
      isDispatched = true,
      dispatchTimestamp = System.currentTimeMillis()
    )
  }

  fun formatEmergencyCadPayload(report: MedicalEmergencyDispatch): String {
    val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US)
    return """
=====================================================
🚨 MEDICAL EMERGENCY CAD DISPATCH REPORT (911 / EMS)
=====================================================
INCIDENT ID:      ${report.incidentId}
TIMESTAMP:        ${df.format(Date(report.timestamp))}
PRIORITY:         ${report.severity}

PATIENT INFO:
  NAME:           ${report.patientName}
  LOCATION/ROOM:  ${report.location}

DEVICE TELEMETRY:
  DEVICE ID:      ${report.deviceId}
  EQUIPMENT:      ${report.deviceType.displayName}
  CURRENT VITALS: ${report.currentVitalsSummary}

INCIDENT DETAILS:
  FAILURE TYPE:   ${report.failureReason}
  RECOMMENDATION: ${report.recommendedMedicalAction}

AUTONOMOUS DISPATCH STATUS: TRANSMITTED TO EMERGENCY GATEWAY
=====================================================
    """.trimIndent()
  }

  companion object {
    /**
     * Generates a 60-sample normalized ECG waveform (P-Q-R-S-T sequence)
     */
    fun generateEcgWaveform(bpm: Int, isFibrillation: Boolean): List<Float> {
      val sampleCount = 60
      val samples = FloatArray(sampleCount) { 0f }

      if (isFibrillation) {
        // Chaotic fibrillation waveform
        val rand = Random(bpm.toLong())
        for (i in 0 until sampleCount) {
          val noise = (sin(i * 0.8) * 0.4 + cos(i * 1.5) * 0.3 + (rand.nextFloat() - 0.5f) * 0.5f).toFloat()
          samples[i] = noise.coerceIn(-1.0f, 1.0f)
        }
        return samples.toList()
      }

      // Standard P-Q-R-S-T model
      val period = (60.0 / bpm.coerceAtLeast(30)) * 1000.0 // ms
      val centerR = sampleCount / 2

      for (i in 0 until sampleCount) {
        val dist = i - centerR
        var valPoint = 0.0

        // Baseline wander / noise
        valPoint += sin(i * 0.2) * 0.03

        // P wave (small upward hump at dist - 14)
        val pDist = dist + 14
        valPoint += 0.15 * exp(-0.5 * (pDist / 2.5).pow(2.0))

        // Q wave (small downward dip at dist - 4)
        val qDist = dist + 4
        valPoint -= 0.18 * exp(-0.5 * (qDist / 1.5).pow(2.0))

        // R wave (sharp tall spike at center)
        valPoint += 1.15 * exp(-0.5 * (dist / 1.6).pow(2.0))

        // S wave (sharp downward dip at dist - 4)
        val sDist = dist - 4
        valPoint -= 0.35 * exp(-0.5 * (sDist / 1.8).pow(2.0))

        // T wave (medium broad hump at dist - 15)
        val tDist = dist - 15
        valPoint += 0.32 * exp(-0.5 * (tDist / 4.0).pow(2.0))

        samples[i] = valPoint.toFloat()
      }

      return samples.toList()
    }
  }
}
