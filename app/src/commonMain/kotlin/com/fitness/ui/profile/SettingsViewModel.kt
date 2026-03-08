package com.fitness.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitness.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SettingsRepository
) : ViewModel() {

    val themeMode: StateFlow<String> = repository.getThemeMode()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    val language: StateFlow<String> = repository.getLanguage()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "en")

    val userQuote: StateFlow<String> = repository.getUserQuote()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Stay fit with FitBot")

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            repository.setThemeMode(mode)
        }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch {
            repository.setLanguage(lang)
        }
    }
    
    fun setUserQuote(quote: String) {
        viewModelScope.launch {
            repository.setUserQuote(quote)
        }
    }
}
