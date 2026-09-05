package com.example.data.dao

import androidx.room.*
import com.example.data.entity.BioMetricData
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for BioMetricData entity providing query, insert, and audit logging methods.
 */
@Dao
interface BioMetricDataDao {

  @Query("SELECT * FROM biometric_data ORDER BY timestamp DESC")
  fun getAllBioMetricData(): Flow<List<BioMetricData>>

  @Query("SELECT * FROM biometric_data WHERE deviceId = :deviceId ORDER BY timestamp DESC")
  fun getBioMetricDataByDevice(deviceId: String): Flow<List<BioMetricData>>

  @Query("SELECT * FROM biometric_data WHERE isEmergency = 1 ORDER BY timestamp DESC")
  fun getEmergencyBioMetricData(): Flow<List<BioMetricData>>

  @Query("SELECT * FROM biometric_data WHERE deviceId LIKE '%' || :query || '%' OR patientName LIKE '%' || :query || '%' OR status LIKE '%' || :query || '%' OR patientRoom LIKE '%' || :query || '%' ORDER BY timestamp DESC")
  fun searchBioMetricData(query: String): Flow<List<BioMetricData>>

  @Query("SELECT COUNT(*) FROM biometric_data")
  fun getBioMetricCount(): Flow<Int>

  @Query("SELECT COUNT(*) FROM biometric_data")
  suspend fun getBioMetricCountDirect(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertBioMetricData(data: BioMetricData): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertBioMetricDataList(dataList: List<BioMetricData>)

  @Query("DELETE FROM biometric_data WHERE id = :id")
  suspend fun deleteBioMetricDataById(id: Long)

  @Query("DELETE FROM biometric_data WHERE deviceId = :deviceId")
  suspend fun deleteBioMetricDataByDeviceId(deviceId: String)

  @Query("DELETE FROM biometric_data")
  suspend fun clearAllBioMetricData()
}
