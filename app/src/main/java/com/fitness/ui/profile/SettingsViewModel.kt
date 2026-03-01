package com.fitness.ui.profile

import android.content.Context
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
    private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    private val LANGUAGE_KEY = stringPreferencesKey("language")
    private val USER_QUOTE_KEY = stringPreferencesKey("user_quote")

    val themeMode = context.dataStore.data
        .map { preferences -> preferences[THEME_MODE_KEY] ?: "system" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    val language = context.dataStore.data
        .map { preferences -> preferences[LANGUAGE_KEY] ?: "zh" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "zh")

    val userQuote = context.dataStore.data
        .map { preferences -> preferences[USER_QUOTE_KEY] ?: "坚持就是胜利" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "坚持就是胜利")

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            context.dataStore.edit { it[THEME_MODE_KEY] = mode }
        }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch {
            context.dataStore.edit { it[LANGUAGE_KEY] = lang }
        }
    }
    
    fun setUserQuote(quote: String) {
        viewModelScope.launch {
            context.dataStore.edit { it[USER_QUOTE_KEY] = quote }
        }
    }
}
