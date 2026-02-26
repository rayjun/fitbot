package com.fitness.sync

import android.content.Context
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fitness.data.local.AppDatabase
import com.fitness.data.local.SetEntity
import com.fitness.model.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val date = inputData.getString("SYNC_DATE") ?: return@withContext Result.failure()
        
        // 1. 获取数据库实例 (实际开发中应使用 DI 注入)
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "fitness-db"
        ).build()
        
        try {
            // 2. 获取当天所有记录
            val sets = db.exerciseDao().getSetsByDate(date)
            if (sets.isEmpty()) return@withContext Result.success()

            // 3. 转换为层级 JSON
            val trainingDay = transformToTrainingDay(date, sets)
            val jsonString = Gson().toJson(trainingDay)

            // 4. 同步到 Google Drive
            // 注意: 这里需要一个已认证的 DriveServiceHelper 实例
            // 为了演示，我们打印 JSON 字符串，实际应用中调用 helper.uploadOrUpdateFile(...)
            println("Syncing to Google Drive: $jsonString")
            
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    /**
     * 将扁平化的 SetEntity 列表转换为层级化的 TrainingDay
     */
    private fun transformToTrainingDay(date: String, sets: List<SetEntity>): TrainingDay {
        val sessions = sets.groupBy { it.sessionId }.map { (sessionId, sessionSets) ->
            val exercises = sessionSets.groupBy { it.exerciseName }.map { (exerciseName, exerciseSets) ->
                val setRecords = exerciseSets.map { 
                    SetRecord(reps = it.reps, weight = it.weight, time = it.timeStr) 
                }
                ExerciseRecord(name = exerciseName, sets = setRecords)
            }
            
            // 简单推断开始和结束时间
            val startTime = sessionSets.first().timeStr
            val endTime = sessionSets.last().timeStr
            
            TrainingSession(
                sessionId = sessionId,
                startTime = startTime,
                endTime = endTime,
                exercises = exercises
            )
        }
        
        return TrainingDay(date = date, sessions = sessions)
    }
}
