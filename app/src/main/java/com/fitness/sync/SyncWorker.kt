package com.fitness.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fitness.data.local.AppDatabase
import com.fitness.data.local.SetEntity
import com.fitness.model.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import java.text.SimpleDateFormat
import java.util.*

class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val gson = Gson()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val date = inputData.getString("SYNC_DATE") ?: return@withContext Result.failure()
        
        // 1. 获取已登录的 Google 账户
        val account = GoogleSignIn.getLastSignedInAccount(applicationContext)
            ?: return@withContext Result.failure()

        // 2. 初始化 Drive 服务
        val credential = GoogleAccountCredential.usingOAuth2(
            applicationContext, listOf(DriveScopes.DRIVE_FILE)
        ).apply {
            selectedAccount = account.account
        }

        val driveService = Drive.Builder(
            NetHttpTransport(),
            GsonFactory(),
            credential
        ).setApplicationName("Fitness Tracker").build()

        val helper = DriveServiceHelper(driveService)
        val db = AppDatabase.getInstance(applicationContext)
        val dao = db.exerciseDao()
        
        try {
            // --- PULL & MERGE ---
            val folderId = helper.getOrCreateFolder("MyFitnessData")
            val remoteJson = helper.downloadFile(folderId, "$date.json")
            
            val existingRemoteIds = dao.getAllRemoteIds().toSet()

            if (remoteJson != null) {
                val remoteDay = gson.fromJson(remoteJson, TrainingDay::class.java)
                val remoteSets = transformToSetEntities(remoteDay)
                
                // 将本地没有的远程记录插入本地数据库
                remoteSets.forEach { remoteSet ->
                    if (!existingRemoteIds.contains(remoteSet.remoteId)) {
                        dao.insertSet(remoteSet)
                    }
                }
            }

            // --- PUSH ---
            // 获取合并后的所有本地记录（包含刚才插入的远程记录）
            val allSets = dao.getSetsByDate(date)
            if (allSets.isEmpty()) return@withContext Result.success()

            val trainingDay = transformToTrainingDay(date, allSets)
            val jsonString = gson.toJson(trainingDay)

            helper.uploadOrUpdateFile(folderId, "$date.json", jsonString)
            
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    /**
     * 将层级化的 TrainingDay 还原为扁平化的 SetEntity 列表
     */
    private fun transformToSetEntities(day: TrainingDay): List<SetEntity> {
        val result = mutableListOf<SetEntity>()
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        day.sessions.forEach { session ->
            session.exercises.forEach { exercise ->
                exercise.sets.forEach { setRecord ->
                    // 解析时间字符串以获取大致的 timestamp
                    val time = try {
                        val parsed = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                            .parse("${day.date} ${setRecord.time}")
                        parsed?.time ?: System.currentTimeMillis()
                    } catch (e: Exception) {
                        System.currentTimeMillis()
                    }

                    result.add(
                        SetEntity(
                            date = day.date,
                            sessionId = session.sessionId,
                            exerciseName = exercise.name,
                            reps = setRecord.reps,
                            weight = setRecord.weight,
                            timestamp = time,
                            timeStr = setRecord.time,
                            remoteId = setRecord.remoteId
                        )
                    )
                }
            }
        }
        return result
    }

    /**
     * 将扁平化的 SetEntity 列表转换为层级化的 TrainingDay
     */
    private fun transformToTrainingDay(date: String, sets: List<SetEntity>): TrainingDay {
        val sessions = sets.groupBy { it.sessionId }.map { (sessionId, sessionSets) ->
            val exercises = sessionSets.groupBy { it.exerciseName }.map { (exerciseName, exerciseSets) ->
                val setRecords = exerciseSets.map { 
                    SetRecord(
                        reps = it.reps, 
                        weight = it.weight, 
                        time = it.timeStr,
                        remoteId = it.remoteId
                    ) 
                }
                ExerciseRecord(name = exerciseName, sets = setRecords)
            }
            
            val startTime = sessionSets.firstOrNull()?.timeStr ?: "00:00"
            val endTime = sessionSets.lastOrNull()?.timeStr ?: "23:59"
            
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
