package com.example.model

enum class BiomedicalDeviceType(
  val displayName: String,
  val defaultFrequency: String,
  val unitCategory: String
) {
  PACEMAKER_IMPLANT("Pacemaker / ICD Implant", "402.5 MHz MedRadio (MICS)", "Cardiology"),
  SMART_INFUSION_PUMP("Smart Infusion Pump", "2.4 GHz Wi-Fi IoT", "ICU / Med-Surg"),
  PULSE_OXIMETER_SPO2("Continuous SpO2 Pulse Oximeter", "BLE 5.2 Medical", "Telemetry"),
  ICU_VENTILATOR("Critical Care ICU Ventilator", "5.8 GHz Medical Telemetry", "ICU / Critical Care"),
  ECG_TELEMETRY_MONITOR("12-Lead Holter / Bedside ECG", "433.92 MHz ISM", "Cardiac Care Unit"),
  GLUCOSE_MONITOR_CGM("Continuous Glucose Monitor (CGM)", "NFC / BLE 2.4G", "Endocrinology"),
  NEURAL_STIMULATOR("Deep Brain / Spinal Neurostimulator", "403.5 MHz MICS", "Neurology")
}

enum class BiomedicalDeviceStatus(
  val displayName: String,
  val isEmergency: Boolean
) {
  NOMINAL("Nominal Operating State", false),
  ABNORMAL_VITALS("Abnormal Patient Vitals Alert", false),
  CRITICAL_FAILURE("Critical Hardware / Sensor Failure", true),
  RF_INTERFERENCE_JAMMED("MedRadio RF Link Jammed / Lost Packets", true),
  MALICIOUS_TAMPER_ATTACK("Malicious Unauthorized Telemetry Override", true),
  BATTERY_DEPLETED("Critically Low Device Battery", true)
}

data class BiomedicalDevice(
  val id: String,
  val patientName: String,
  val patientRoom: String,
  val patientAge: Int = 58,
  val deviceType: BiomedicalDeviceType,
  val rfBandFrequency: String = deviceType.defaultFrequency,
  val hospitalId: String = "UMC-MAIN",
  val hospitalName: String = "University Medical Center",
  val biomedicalPartner: String = "Medtronic / Philips Healthcare",
  val hospitalAuthorizationGranted: Boolean = true,
  val latitude: Double = 37.7749,
  val longitude: Double = -122.4194,
  val heartRateBpm: Int = 72,
  val spO2Percent: Int = 98,
  val bloodPressureSys: Int = 120,
  val bloodPressureDia: Int = 80,
  val glucoseMgDl: Int = 105,
  val infusionRateMlH: Float = 25.0f,
  val respirationRateBpm: Int = 16,
  val batteryPercent: Int = 92,
  val rssiDbm: Double = -64.0,
  val packetLossPercent: Float = 0.0f,
  val status: BiomedicalDeviceStatus = BiomedicalDeviceStatus.NOMINAL,
  val anomalyDescription: String? = null,
  val lastTelemetryTimestamp: Long = System.currentTimeMillis(),
  val emergencyContactRecommended: Boolean = false,
  val ecgSamples: List<Float> = emptyList(),
  val vitalsHistory: List<Int> = emptyList()
)

data class MedicalEmergencyDispatch(
  val incidentId: String = java.util.UUID.randomUUID().toString().take(8).uppercase(),
  val timestamp: Long = System.currentTimeMillis(),
  val patientName: String,
  val location: String,
  val deviceId: String,
  val deviceType: BiomedicalDeviceType,
  val currentVitalsSummary: String,
  val severity: String = "CRITICAL - IMMEDIATE DISPATCH",
  val failureReason: String,
  val recommendedMedicalAction: String,
  val emergencyPhoneNumber: String = "911",
  val isDispatched: Boolean = false,
  val dispatchTimestamp: Long? = null
)
