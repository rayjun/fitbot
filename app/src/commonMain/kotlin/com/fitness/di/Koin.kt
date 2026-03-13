package com.fitness.di

import com.fitness.data.DataStoreRepository
import com.fitness.data.SettingsRepository
import com.fitness.data.WorkoutRepository
import com.fitness.data.AiRepository
import com.fitness.data.KtorAiRepository
import com.fitness.ui.plans.PlanViewModel
import com.fitness.ui.profile.ProfileViewModel
import com.fitness.ui.profile.SettingsViewModel
import com.fitness.ui.workout.WorkoutViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val commonModule = module {
    // DataStoreRepository implements both WorkoutRepository and SettingsRepository.
    // Declare a single shared instance and bind it to both interfaces.
    single { DataStoreRepository(get()) }
    single<WorkoutRepository> { get<DataStoreRepository>() }
    single<SettingsRepository> { get<DataStoreRepository>() }
    single<AiRepository> { KtorAiRepository() }

    factory { PlanViewModel(get()) }
    factory { WorkoutViewModel(get()) }
    factory { SettingsViewModel(get()) }
    factory { ProfileViewModel(get(), get(), get()) }
}

expect val platformModule: Module

fun initKoin(appDeclaration: KoinAppDeclaration = {}) {
    // Avoid double start
    try {
        startKoin {
            appDeclaration()
            modules(commonModule, platformModule)
        }
    } catch (e: Exception) {
        // Already started
    }
}

// iOS helper — call setupKoin() from Swift AppDelegate instead of using this directly
fun initKoin() = initKoin {}
