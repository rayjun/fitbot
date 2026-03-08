package com.fitness.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.fitness.util.dataStore
import org.koin.core.module.Module
import org.koin.dsl.module

import com.fitness.data.RoomWorkoutRepository
import com.fitness.data.SettingsRepository
import com.fitness.data.WorkoutRepository
import com.fitness.data.local.AppDatabase
import com.fitness.data.local.ExerciseDao
import com.fitness.data.local.PlanDao

actual val platformModule: Module = module {
    single<DataStore<Preferences>> {
        val context: Context = get()
        context.dataStore
    }

    single { AppDatabase.getInstance(get()) }
    single { get<AppDatabase>().exerciseDao() }
    single { get<AppDatabase>().planDao() }

    // RoomWorkoutRepository implements both WorkoutRepository and SettingsRepository on Android.
    // Share a single instance bound to both interfaces.
    single { RoomWorkoutRepository(get(), get(), get()) }
    single<WorkoutRepository> { get<RoomWorkoutRepository>() }
    single<SettingsRepository> { get<RoomWorkoutRepository>() }
}
