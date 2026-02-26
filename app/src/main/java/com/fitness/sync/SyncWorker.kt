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

import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes

class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val date = inputData.getString("SYNC_DATE") ?: return@withContext Result.failure()
        
        // 1. 获取已登录的 Google 账户
        val account = GoogleSignIn.getLastSignedInAccount(applicationContext)
            ?: return@withContext Result.failure() // 未登录则同步失败

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
        
        try {
            val sets = db.exerciseDao().getSetsByDate(date)
            if (sets.isEmpty()) return@withContext Result.success()

            val trainingDay = transformToTrainingDay(date, sets)
            val jsonString = Gson().toJson(trainingDay)

            // 3. 执行真实上传
            val folderId = helper.getOrCreateFolder("MyFitnessData")
            helper.uploadOrUpdateFile(folderId, "$date.json", jsonString)
            
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
