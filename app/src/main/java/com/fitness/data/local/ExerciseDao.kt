package com.fitness.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ExerciseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(set: SetEntity)

    @androidx.room.Update
    suspend fun updateSet(set: SetEntity)

    @Query("SELECT * FROM exercise_sets WHERE date = :date ORDER BY timestamp ASC")
    fun getSetsByDateFlow(date: String): kotlinx.coroutines.flow.Flow<List<SetEntity>>

    @Query("SELECT * FROM exercise_sets WHERE date = :date ORDER BY timestamp ASC")
    suspend fun getSetsByDate(date: String): List<SetEntity>

    @Query("SELECT DISTINCT date FROM exercise_sets")
    suspend fun getDistinctDates(): List<String>

    @Query("SELECT DISTINCT date FROM exercise_sets WHERE timestamp > :since")
    suspend fun getDistinctDatesModifiedSince(since: Long): List<String>

    @Query("DELETE FROM exercise_sets WHERE id = :id")
    suspend fun deleteSet(id: Long)

    @Query("SELECT * FROM exercise_sets ORDER BY timestamp DESC")
    fun getAllSetsFlow(): kotlinx.coroutines.flow.Flow<List<SetEntity>>

    @Query("SELECT remoteId FROM exercise_sets")
    suspend fun getAllRemoteIds(): List<String>
}
