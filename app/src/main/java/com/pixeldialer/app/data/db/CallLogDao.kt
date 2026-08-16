package com.pixeldialer.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CallLogDao {

    @Query("SELECT * FROM call_log ORDER BY timestampMillis DESC")
    fun observeAll(): Flow<List<CallLogEntity>>

    @Query("SELECT * FROM call_log WHERE direction = 'MISSED' ORDER BY timestampMillis DESC")
    fun observeMissed(): Flow<List<CallLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: CallLogEntity): Long

    @Delete
    suspend fun delete(entry: CallLogEntity)

    @Query("DELETE FROM call_log")
    suspend fun clearAll()
}
