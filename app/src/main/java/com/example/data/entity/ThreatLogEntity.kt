package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Database entity representing a detected RF or FSO signal threat incident.
 */
@Entity(tableName = "threat_events")
data class ThreatLogEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val eventId: String,
  val timestamp: Long = System.currentTimeMillis(),
  val source: String, // "RF" or "FSO"
  val band: String,
  val powerDbm: Double,
  val thresholdDbm: Double,
  val threatType: String,
  val severity: String, // "CRITICAL", "HIGH", "MEDIUM", "INFO"
  val details: String,
  val actionTaken: String,
  val notes: String = ""
)
