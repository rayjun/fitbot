package com.fitness.ui.profile

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsViewModel(private val context: Context) : ViewModel() {
    private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
    private val LANGUAGE_KEY = stringPreferencesKey("language")

    val isDarkMode = context.dataStore.data
        .map { preferences -> preferences[DARK_MODE_KEY] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val language = context.dataStore.data
        .map { preferences -> preferences[LANGUAGE_KEY] ?: "zh" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "zh")

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { it[DARK_MODE_KEY] = enabled }
        }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch {
            context.dataStore.edit { it[LANGUAGE_KEY] = lang }
        }
    }
}
