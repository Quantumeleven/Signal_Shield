package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.BioMetricDataDao
import com.example.data.dao.SystemEventDao
import com.example.data.dao.ThreatLogDao
import com.example.data.entity.BioMetricData
import com.example.data.entity.SystemEventEntity
import com.example.data.entity.ThreatLogEntity

@Database(
  entities = [ThreatLogEntity::class, SystemEventEntity::class, BioMetricData::class],
  version = 2,
  exportSchema = false
)
abstract class SignalShieldDatabase : RoomDatabase() {
  abstract fun threatLogDao(): ThreatLogDao
  abstract fun systemEventDao(): SystemEventDao
  abstract fun bioMetricDataDao(): BioMetricDataDao

  companion object {
    @Volatile
    private var INSTANCE: SignalShieldDatabase? = null

    fun getDatabase(context: Context): SignalShieldDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          SignalShieldDatabase::class.java,
          "signal_shield_threat_log.db"
        )
          .fallbackToDestructiveMigration()
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
