package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Database entity representing system events (WebSocket changes, shutter activations, scans, AI analyses).
 */
@Entity(tableName = "system_events")
data class SystemEventEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val timestamp: Long = System.currentTimeMillis(),
  val category: String, // "CONNECTION", "SHIELD", "SCAN", "CONFIG", "AI_ANALYSIS"
  val title: String,
  val message: String,
  val severity: String, // "INFO", "WARNING", "ERROR", "SUCCESS"
  val targetEndpoint: String = ""
)
