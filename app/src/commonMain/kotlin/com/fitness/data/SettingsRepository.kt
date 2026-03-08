package com.fitness.data

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getThemeMode(): Flow<String>
    suspend fun setThemeMode(mode: String)

    fun getLanguage(): Flow<String>
    suspend fun setLanguage(lang: String)

    fun getUserQuote(): Flow<String>
    suspend fun setUserQuote(quote: String)
}
