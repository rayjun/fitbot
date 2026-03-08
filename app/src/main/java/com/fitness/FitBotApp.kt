package com.fitness

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.fitness.di.commonModule
import com.fitness.di.platformModule
import dagger.hilt.android.HiltAndroidApp
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import javax.inject.Inject

@HiltAndroidApp
class FitBotApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@FitBotApp)
            modules(commonModule, platformModule)
        }
    }

    override val workManagerConfiguration: Configuration
        get() {
            Log.i("FitBotApp", "Providing custom WorkManager configuration with HiltWorkerFactory")
            return Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build()
        }
}
