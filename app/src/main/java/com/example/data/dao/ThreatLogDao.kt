package com.example.data.dao

import androidx.room.*
import com.example.data.entity.ThreatLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ThreatLogDao {
  @Query("SELECT * FROM threat_events ORDER BY timestamp DESC")
  fun getAllThreats(): Flow<List<ThreatLogEntity>>

  @Query("SELECT * FROM threat_events WHERE severity = :severity ORDER BY timestamp DESC")
  fun getThreatsBySeverity(severity: String): Flow<List<ThreatLogEntity>>

  @Query("SELECT * FROM threat_events WHERE source = :source ORDER BY timestamp DESC")
  fun getThreatsBySource(source: String): Flow<List<ThreatLogEntity>>

  @Query("SELECT * FROM threat_events WHERE threatType LIKE '%' || :query || '%' OR band LIKE '%' || :query || '%' OR details LIKE '%' || :query || '%' ORDER BY timestamp DESC")
  fun searchThreats(query: String): Flow<List<ThreatLogEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertThreat(threat: ThreatLogEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertThreats(threats: List<ThreatLogEntity>)

  @Query("DELETE FROM threat_events WHERE id = :id")
  suspend fun deleteThreatById(id: Long)

  @Query("DELETE FROM threat_events")
  suspend fun clearAllThreats()

  @Query("SELECT COUNT(*) FROM threat_events")
  fun getThreatCount(): Flow<Int>

  @Query("SELECT COUNT(*) FROM threat_events WHERE severity = 'CRITICAL'")
  fun getCriticalThreatCount(): Flow<Int>

  @Query("SELECT COUNT(*) FROM threat_events")
  suspend fun getThreatCountDirect(): Int
}
