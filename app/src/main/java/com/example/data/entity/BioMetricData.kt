package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Database entity representing incoming physiological telemetry for medical audit trails.
 * Captures historical physiological data, timestamps, device IDs, status flags, and telemetry diagnostics.
 */
@Entity(tableName = "biometric_data")
data class BioMetricData(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val timestamp: Long = System.currentTimeMillis(),
  val deviceId: String,
  val patientName: String,
  val patientRoom: String,
  val deviceType: String,
  val heartRateBpm: Int,
  val spO2Percent: Int,
  val bloodPressureSys: Int,
  val bloodPressureDia: Int,
  val respirationRateBpm: Int = 16,
  val glucoseMgDl: Int = 100,
  val infusionRateMlH: Float = 0f,
  val batteryPercent: Int = 100,
  val rssiDbm: Double = -65.0,
  val packetLossPercent: Float = 0f,
  val status: String = "NOMINAL",
  val isEmergency: Boolean = false,
  val anomalyDescription: String? = null,
  val auditNotes: String = ""
)
