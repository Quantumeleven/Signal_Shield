package com.example.data.dao

import androidx.room.*
import com.example.data.entity.SystemEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SystemEventDao {
  @Query("SELECT * FROM system_events ORDER BY timestamp DESC")
  fun getAllEvents(): Flow<List<SystemEventEntity>>

  @Query("SELECT * FROM system_events WHERE category = :category ORDER BY timestamp DESC")
  fun getEventsByCategory(category: String): Flow<List<SystemEventEntity>>

  @Query("SELECT * FROM system_events WHERE title LIKE '%' || :query || '%' OR message LIKE '%' || :query || '%' ORDER BY timestamp DESC")
  fun searchEvents(query: String): Flow<List<SystemEventEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertEvent(event: SystemEventEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertEvents(events: List<SystemEventEntity>)

  @Query("DELETE FROM system_events WHERE id = :id")
  suspend fun deleteEventById(id: Long)

  @Query("DELETE FROM system_events")
  suspend fun clearAllEvents()

  @Query("SELECT COUNT(*) FROM system_events")
  fun getEventCount(): Flow<Int>

  @Query("SELECT COUNT(*) FROM system_events")
  suspend fun getEventCountDirect(): Int
}
