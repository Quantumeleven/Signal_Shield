package com.example.model

/**
 * Settings & System Compliance Configuration
 * Supports FCC Regulation enforcement, emergency contacts/location dispatch,
 * and HIPAA / biomedical authorization controls.
 */
data class EmergencyContact(
  val id: String,
  val serviceName: String,
  val category: String, // e.g., "911 EMS Dispatch", "Hospital Trauma Center", "Device Biomedical Engineering", "FCC Spectrum Enforcement"
  val phoneNumber: String,
  val frequencyOrRadio: String = "VHF 155.340 MHz (HEAR / MED-9)",
  val isPrimary: Boolean = false,
  val hospitalAffiliation: String? = null
)

data class HospitalAuthorizationRecord(
  val hospitalId: String,
  val hospitalName: String,
  val biomedicalPartner: String,
  val hipaaBaaStatus: String, // "ACTIVE_BAA_SIGNED", "PENDING_VERIFICATION", "REVOKED"
  val authorizedPatientIds: List<String>,
  val thirdPartyViewingPermitted: Boolean,
  val remoteMitigationPermitted: Boolean,
  val signalLocationTrackingPermitted: Boolean,
  val verificationTimestamp: Long = System.currentTimeMillis()
)

data class FccRegulationConfig(
  val enforceNonSimulatedHardware: Boolean = true,
  val fccRuleParts: List<String> = listOf("Part 15 (Unlicensed ISM)", "Part 95 Subpart I (MedRadio)", "Part 90 (Public Safety)"),
  val strictInterferenceShielding: Boolean = true,
  val certifiedOperatorCallsign: String = "FCC-STA-2026-MED",
  val autoReportRogueTransmitters: Boolean = true,
  val emergencyBeaconMonitoring: Boolean = true
)

data class LocationTrackingSettings(
  val gpsLocationEnabled: Boolean = true,
  val lastKnownLatitude: Double = 37.7749,
  val lastKnownLongitude: Double = -122.4194,
  val locationAccuracyMeters: Float = 4.2f,
  val hospitalGeofenceZone: String = "St. Jude / UCSF Medical District Geofence",
  val emergencyLocationSharingActive: Boolean = true
)

data class AppSystemSettings(
  val locationSettings: LocationTrackingSettings = LocationTrackingSettings(),
  val fccConfig: FccRegulationConfig = FccRegulationConfig(),
  val emergencyContacts: List<EmergencyContact> = listOf(
    EmergencyContact(
      id = "EMERG-911",
      serviceName = "Municipal 911 / EMS CAD Dispatch",
      category = "Emergency Medical Services",
      phoneNumber = "911",
      frequencyOrRadio = "HEAR 155.340 MHz / APCO P25",
      isPrimary = true
    ),
    EmergencyContact(
      id = "HOSP-TRAUMA-1",
      serviceName = "Metropolitan Level 1 Trauma & Code Blue Resuscitation",
      category = "Hospital Emergency Dept",
      phoneNumber = "(800) 555-0199",
      frequencyOrRadio = "MED-4 463.075 MHz",
      hospitalAffiliation = "University Medical Center"
    ),
    EmergencyContact(
      id = "BIOMED-ENG-MEDTRONIC",
      serviceName = "Biomedical Engineering & Implant Rapid Response",
      category = "Device OEM Response",
      phoneNumber = "(800) 555-0144",
      frequencyOrRadio = "Direct Cloud Telemetry API / TLS 1.3",
      hospitalAffiliation = "Medtronic & Abbott Bio-Response"
    ),
    EmergencyContact(
      id = "FCC-ESB",
      serviceName = "FCC Public Safety & Homeland Security Bureau",
      category = "Federal Spectrum Enforcement",
      phoneNumber = "(888) 225-5322",
      frequencyOrRadio = "Interference Investigation Field Desk"
    )
  ),
  val hospitalAuthorizations: List<HospitalAuthorizationRecord> = listOf(
    HospitalAuthorizationRecord(
      hospitalId = "UMC-MAIN",
      hospitalName = "University Medical Center & CCU",
      biomedicalPartner = "Medtronic / Philips Healthcare",
      hipaaBaaStatus = "ACTIVE_BAA_SIGNED",
      authorizedPatientIds = listOf("MED-PAC-8041", "MED-PUMP-209", "MED-VENT-550"),
      thirdPartyViewingPermitted = true,
      remoteMitigationPermitted = true,
      signalLocationTrackingPermitted = true
    ),
    HospitalAuthorizationRecord(
      hospitalId = "ST-MARYS-COMMUNITY",
      hospitalName = "St. Mary's Regional Hospital",
      biomedicalPartner = "Baxter / GE Healthcare",
      hipaaBaaStatus = "ACTIVE_BAA_SIGNED",
      authorizedPatientIds = listOf("MED-ECG-118", "MED-CGM-902"),
      thirdPartyViewingPermitted = true,
      remoteMitigationPermitted = true,
      signalLocationTrackingPermitted = true
    ),
    HospitalAuthorizationRecord(
      hospitalId = "MEMORIAL-CARDIO",
      hospitalName = "Memorial Cardiovascular Institute",
      biomedicalPartner = "Abbott Medical / Boston Scientific",
      hipaaBaaStatus = "PENDING_VERIFICATION",
      authorizedPatientIds = listOf("MED-SPO2-334"),
      thirdPartyViewingPermitted = false,
      remoteMitigationPermitted = false,
      signalLocationTrackingPermitted = false
    )
  ),
  val privacyPolicyAccepted: Boolean = true,
  val thirdPartyHospitalViewingActive: Boolean = true,
  val hipaaMaskingEnabled: Boolean = false // If true without authorization, mask patient names
)
