package com.example.data

import com.example.data.dao.BioMetricDataDao
import com.example.data.dao.SystemEventDao
import com.example.data.dao.ThreatLogDao
import com.example.data.entity.BioMetricData
import com.example.data.entity.SystemEventEntity
import com.example.data.entity.ThreatLogEntity
import com.example.model.BiomedicalDevice
import com.example.model.DetectionEvent
import com.example.model.EventSeverity
import com.example.model.SignalSource
import kotlinx.coroutines.flow.Flow

class ThreatLogRepository(
  private val threatLogDao: ThreatLogDao,
  private val systemEventDao: SystemEventDao,
  private val bioMetricDataDao: BioMetricDataDao
) {
  val allThreats: Flow<List<ThreatLogEntity>> = threatLogDao.getAllThreats()
  val allSystemEvents: Flow<List<SystemEventEntity>> = systemEventDao.getAllEvents()
  val allBioMetricData: Flow<List<BioMetricData>> = bioMetricDataDao.getAllBioMetricData()
  val threatCount: Flow<Int> = threatLogDao.getThreatCount()
  val criticalThreatCount: Flow<Int> = threatLogDao.getCriticalThreatCount()
  val systemEventCount: Flow<Int> = systemEventDao.getEventCount()
  val bioMetricCount: Flow<Int> = bioMetricDataDao.getBioMetricCount()

  fun getThreatsBySeverity(severity: String): Flow<List<ThreatLogEntity>> =
    threatLogDao.getThreatsBySeverity(severity)

  fun getThreatsBySource(source: String): Flow<List<ThreatLogEntity>> =
    threatLogDao.getThreatsBySource(source)

  fun searchThreats(query: String): Flow<List<ThreatLogEntity>> =
    threatLogDao.searchThreats(query)

  fun getSystemEventsByCategory(category: String): Flow<List<SystemEventEntity>> =
    systemEventDao.getEventsByCategory(category)

  fun searchSystemEvents(query: String): Flow<List<SystemEventEntity>> =
    systemEventDao.searchEvents(query)

  fun getBioMetricDataByDevice(deviceId: String): Flow<List<BioMetricData>> =
    bioMetricDataDao.getBioMetricDataByDevice(deviceId)

  fun searchBioMetricData(query: String): Flow<List<BioMetricData>> =
    bioMetricDataDao.searchBioMetricData(query)

  fun getEmergencyBioMetricData(): Flow<List<BioMetricData>> =
    bioMetricDataDao.getEmergencyBioMetricData()

  suspend fun recordBioMetricTelemetry(
    device: BiomedicalDevice,
    notes: String = ""
  ): Long {
    val entity = BioMetricData(
      timestamp = System.currentTimeMillis(),
      deviceId = device.id,
      patientName = device.patientName,
      patientRoom = device.patientRoom,
      deviceType = device.deviceType.displayName,
      heartRateBpm = device.heartRateBpm,
      spO2Percent = device.spO2Percent,
      bloodPressureSys = device.bloodPressureSys,
      bloodPressureDia = device.bloodPressureDia,
      respirationRateBpm = device.respirationRateBpm,
      glucoseMgDl = device.glucoseMgDl,
      infusionRateMlH = device.infusionRateMlH,
      batteryPercent = device.batteryPercent,
      rssiDbm = device.rssiDbm,
      packetLossPercent = device.packetLossPercent,
      status = device.status.displayName,
      isEmergency = device.status.isEmergency || device.emergencyContactRecommended,
      anomalyDescription = device.anomalyDescription,
      auditNotes = notes
    )
    return bioMetricDataDao.insertBioMetricData(entity)
  }

  suspend fun recordBioMetricData(data: BioMetricData): Long {
    return bioMetricDataDao.insertBioMetricData(data)
  }

  suspend fun recordThreat(
    detectionEvent: DetectionEvent,
    notes: String = ""
  ): Long {
    val entity = ThreatLogEntity(
      eventId = detectionEvent.id,
      timestamp = detectionEvent.timestamp,
      source = if (detectionEvent.source == SignalSource.FSO) "FSO" else "RF",
      band = detectionEvent.band,
      powerDbm = detectionEvent.powerDbm,
      thresholdDbm = detectionEvent.thresholdDbm,
      threatType = detectionEvent.threatType,
      severity = detectionEvent.severity.name,
      details = detectionEvent.details,
      actionTaken = detectionEvent.actionTaken,
      notes = notes
    )
    return threatLogDao.insertThreat(entity)
  }

  suspend fun recordSystemEvent(
    category: String,
    title: String,
    message: String,
    severity: String = "INFO",
    targetEndpoint: String = ""
  ): Long {
    val entity = SystemEventEntity(
      timestamp = System.currentTimeMillis(),
      category = category,
      title = title,
      message = message,
      severity = severity,
      targetEndpoint = targetEndpoint
    )
    return systemEventDao.insertEvent(entity)
  }

  suspend fun seedInitialHistoryIfEmpty() {
    if (threatLogDao.getThreatCountDirect() == 0) {
      val now = System.currentTimeMillis()
      val sampleThreats = listOf(
        ThreatLogEntity(
          eventId = "EVT-89214",
          timestamp = now - 3600000 * 2 - 120000,
          source = "FSO",
          band = "1550 nm (C-Band)",
          powerDbm = -8.2,
          thresholdDbm = -20.0,
          threatType = "DIRECT BLINDING LASER SPIKE",
          severity = "CRITICAL",
          details = "High-irradiance continuous wave laser burst detected on quadrant photodiode sensor.",
          actionTaken = "AUTO_BLOCKED (Shutter Ch 1550nm engaged in 2.1ms)"
        ),
        ThreatLogEntity(
          eventId = "EVT-89190",
          timestamp = now - 3600000 * 4 - 450000,
          source = "RF",
          band = "2.4 GHz (ISM Ch 6)",
          powerDbm = -42.5,
          thresholdDbm = -60.0,
          threatType = "WIDEBAND BARRAGE JAMMER",
          severity = "CRITICAL",
          details = "High-energy non-OFDM chirp jamming detected across 802.11 spectrum channels 1-11.",
          actionTaken = "ALERT_TRIGGERED (Dynamic Notch Filter Engaged)"
        ),
        ThreatLogEntity(
          eventId = "EVT-88931",
          timestamp = now - 3600000 * 7 - 180000,
          source = "FSO",
          band = "905 nm (LiDAR)",
          powerDbm = -18.4,
          thresholdDbm = -25.0,
          threatType = "PULSED TIME-OF-FLIGHT SPOOF",
          severity = "HIGH",
          details = "Asynchronous high-repetition LiDAR pulse train injected into autonomous receiver.",
          actionTaken = "SHIELD_ENGAGED (Spectral Bandpass Attenuator On)"
        ),
        ThreatLogEntity(
          eventId = "EVT-88402",
          timestamp = now - 3600000 * 12 - 500000,
          source = "RF",
          band = "433 MHz (ISM/UHF)",
          powerDbm = -68.0,
          thresholdDbm = -80.0,
          threatType = "UNAUTHORIZED CARRIER EMISSION",
          severity = "MEDIUM",
          details = "Unregistered continuous wave transmitter active in restricted low-power ISM sub-band.",
          actionTaken = "LOGGED (Direction Finding Triangulation Queued)"
        ),
        ThreatLogEntity(
          eventId = "EVT-87890",
          timestamp = now - 3600000 * 24 - 100000,
          source = "RF",
          band = "5.8 GHz (UNII-3)",
          powerDbm = -54.0,
          thresholdDbm = -60.0,
          threatType = "DRONE C2 LINK ANOMALY",
          severity = "HIGH",
          details = "FHSS telemetry burst with anomalous hopping sequence matching rogue UAV protocol.",
          actionTaken = "RECORDED (SigMF I/Q Snapshot Archived)"
        )
      )
      threatLogDao.insertThreats(sampleThreats)
    }

    if (systemEventDao.getEventCountDirect() == 0) {
      val now = System.currentTimeMillis()
      val sampleEvents = listOf(
        SystemEventEntity(
          timestamp = now - 3600000 * 2 - 110000,
          category = "SHIELD",
          title = "Autonomous Shutter Triggered",
          message = "Photonic shutter automatically closed for 1550nm band following threshold violation.",
          severity = "WARNING"
        ),
        SystemEventEntity(
          timestamp = now - 3600000 * 5,
          category = "CONNECTION",
          title = "GNU Radio Bridge Synchronized",
          message = "WebSocket telemetry pipe established with GNU Radio live flowgraph sink.",
          severity = "SUCCESS",
          targetEndpoint = "ws://10.0.2.2:8765"
        ),
        SystemEventEntity(
          timestamp = now - 3600000 * 8,
          category = "AI_ANALYSIS",
          title = "Gemini SigInt Anomaly Report",
          message = "Model evaluated 12 spectrum frames: nominal noise floor with isolated 2.4 GHz spike.",
          severity = "INFO"
        ),
        SystemEventEntity(
          timestamp = now - 3600000 * 24,
          category = "CONFIG",
          title = "Shield Thresholds Configured",
          message = "RF Alert set to -80 dBm, High to -60 dBm. FSO Alert set to -40 dBm, Block to -20 dBm.",
          severity = "INFO"
        )
      )
      systemEventDao.insertEvents(sampleEvents)
    }

    if (bioMetricDataDao.getBioMetricCountDirect() == 0) {
      val now = System.currentTimeMillis()
      val sampleBioMetrics = listOf(
        BioMetricData(
          timestamp = now - 3600000 * 1 - 250000,
          deviceId = "MED-PAC-8041",
          patientName = "Eleanor Vance",
          patientRoom = "ICU-Bed-04",
          deviceType = "Pacemaker / ICD Implant",
          heartRateBpm = 74,
          spO2Percent = 98,
          bloodPressureSys = 122,
          bloodPressureDia = 82,
          respirationRateBpm = 16,
          glucoseMgDl = 105,
          batteryPercent = 94,
          rssiDbm = -62.0,
          packetLossPercent = 0.0f,
          status = "Nominal Operating State",
          isEmergency = false,
          anomalyDescription = null,
          auditNotes = "Routine hourly automated telemetry sync. Cardiac parameters within normal limits."
        ),
        BioMetricData(
          timestamp = now - 3600000 * 2 - 120000,
          deviceId = "MED-PUMP-209",
          patientName = "Robert Chen",
          patientRoom = "StepDown-Rm-12",
          deviceType = "Smart Infusion Pump",
          heartRateBpm = 82,
          spO2Percent = 97,
          bloodPressureSys = 128,
          bloodPressureDia = 84,
          infusionRateMlH = 25.0f,
          batteryPercent = 88,
          rssiDbm = -58.0,
          packetLossPercent = 0.0f,
          status = "Nominal Operating State",
          isEmergency = false,
          anomalyDescription = null,
          auditNotes = "Infusion channel A running regular dosage (0.9% NaCl)."
        ),
        BioMetricData(
          timestamp = now - 3600000 * 3 - 45000,
          deviceId = "MED-VENT-550",
          patientName = "Marcus Aurelius",
          patientRoom = "Trauma-Bay-01",
          deviceType = "Critical Care ICU Ventilator",
          heartRateBpm = 88,
          spO2Percent = 96,
          bloodPressureSys = 135,
          bloodPressureDia = 88,
          respirationRateBpm = 18,
          batteryPercent = 99,
          rssiDbm = -55.0,
          packetLossPercent = 0.0f,
          status = "Nominal Operating State",
          isEmergency = false,
          anomalyDescription = null,
          auditNotes = "SIMV+PS mode active; tidal volume and peak inspiratory pressure normal."
        ),
        BioMetricData(
          timestamp = now - 3600000 * 5 - 180000,
          deviceId = "MED-OXI-104",
          patientName = "Sarah Jenkins",
          patientRoom = "Cardio-West-08",
          deviceType = "Continuous SpO2 Pulse Oximeter",
          heartRateBpm = 68,
          spO2Percent = 99,
          bloodPressureSys = 118,
          bloodPressureDia = 78,
          respirationRateBpm = 14,
          batteryPercent = 76,
          rssiDbm = -68.0,
          packetLossPercent = 0.0f,
          status = "Nominal Operating State",
          isEmergency = false,
          anomalyDescription = null,
          auditNotes = "Continuous PPG pulse waveform calibrated and verified."
        )
      )
      bioMetricDataDao.insertBioMetricDataList(sampleBioMetrics)
    }
  }

  suspend fun deleteThreat(id: Long) = threatLogDao.deleteThreatById(id)

  suspend fun clearAllThreats() = threatLogDao.clearAllThreats()

  suspend fun deleteSystemEvent(id: Long) = systemEventDao.deleteEventById(id)

  suspend fun clearAllSystemEvents() = systemEventDao.clearAllEvents()

  suspend fun deleteBioMetricData(id: Long) = bioMetricDataDao.deleteBioMetricDataById(id)

  suspend fun deleteBioMetricDataByDevice(deviceId: String) = bioMetricDataDao.deleteBioMetricDataByDeviceId(deviceId)

  suspend fun clearAllBioMetricData() = bioMetricDataDao.clearAllBioMetricData()
}
