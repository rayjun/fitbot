package com.fitness.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitness.data.AnalyticsEngine
import com.fitness.data.WorkoutRepository
import com.fitness.data.SettingsRepository
import com.fitness.data.AiRepository
import com.fitness.model.AiChatMessage
import com.fitness.util.AiPromptBuilder
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

enum class TimeRange {
    WEEK, MONTH, YEAR
}

class ProfileViewModel(
    private val repository: WorkoutRepository,
    private val settingsRepository: SettingsRepository,
    private val aiRepository: AiRepository
) : ViewModel() {

    val heatmapData: StateFlow<Map<String, Int>> = repository.getHeatmapData()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    private val _selectedTimeRange = MutableStateFlow(TimeRange.MONTH)
    val selectedTimeRange: StateFlow<TimeRange> = _selectedTimeRange

    // AI Insight State (Static summary)
    private val _aiInsight = MutableStateFlow<String?>(null)
    val aiInsight: StateFlow<String?> = _aiInsight

    private val _isGeneratingInsight = MutableStateFlow(false)
    val isGeneratingInsight: StateFlow<Boolean> = _isGeneratingInsight

    // AI Coach Chat State
    private val _chatMessages = MutableStateFlow<List<AiChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<AiChatMessage>> = _chatMessages

    private val _isChatProcessing = MutableStateFlow(false)
    val isChatProcessing: StateFlow<Boolean> = _isChatProcessing

    val muscleVolumeData: StateFlow<Map<String, Double>> = combine(
        repository.getAllSets(),
        _selectedCategory,
        _selectedTimeRange
    ) { sets, category, range ->
        val filteredSets = filterSetsByRange(sets, range)
        val aggregated = AnalyticsEngine.calculateVolumePerMuscleGroup(filteredSets)
        
        if (category == null) {
            aggregated
        } else {
            aggregated.filter { it.key == category }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private fun filterSetsByRange(sets: List<com.fitness.model.ExerciseSet>, range: TimeRange): List<com.fitness.model.ExerciseSet> {
        val now = Clock.System.now()
        val filterDate = when (range) {
            TimeRange.WEEK -> now.minus(7, DateTimeUnit.DAY, TimeZone.currentSystemDefault())
            TimeRange.MONTH -> now.minus(30, DateTimeUnit.DAY, TimeZone.currentSystemDefault())
            TimeRange.YEAR -> now.minus(365, DateTimeUnit.DAY, TimeZone.currentSystemDefault())
        }.toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
        return sets.filter { it.date >= filterDate }
    }

    fun setSelectedCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun setSelectedTimeRange(range: TimeRange) {
        _selectedTimeRange.value = range
        _aiInsight.value = null
    }

    fun generateAiInsight(language: String) {
        viewModelScope.launch {
            val apiKey = settingsRepository.getAiApiKey().first()
            val baseUrl = settingsRepository.getAiBaseUrl().first()
            val model = settingsRepository.getAiModel().first()
            
            if (apiKey.isEmpty()) {
                _aiInsight.value = if (language == "zh") "请先在设置中配置 AI API Key。" else "Please configure AI API Key in settings first."
                return@launch
            }

            _isGeneratingInsight.value = true
            val sets = repository.getAllSets().first()
            // Insights are always based on the currently selected time range
            val filteredSets = filterSetsByRange(sets, _selectedTimeRange.value)
            
            val messages = AiPromptBuilder.buildSummaryPrompt(filteredSets, language)
            val result = aiRepository.getChatCompletion(baseUrl, apiKey, model, messages)
            
            result.onSuccess { _aiInsight.value = it }.onFailure { _aiInsight.value = "Error: ${it.message}" }
            _isGeneratingInsight.value = false
        }
    }

    fun sendChatMessage(text: String, language: String) {
        viewModelScope.launch {
            val apiKey = settingsRepository.getAiApiKey().first()
            val baseUrl = settingsRepository.getAiBaseUrl().first()
            val model = settingsRepository.getAiModel().first()

            if (apiKey.isEmpty()) {
                _chatMessages.value = _chatMessages.value + AiChatMessage("assistant", if (language == "zh") "请先在设置中配置 AI API Key。" else "Please configure AI API Key in settings first.")
                return@launch
            }

            // 1. Add user message to UI
            val userMsg = AiChatMessage("user", text)
            _chatMessages.value = _chatMessages.value + userMsg
            _isChatProcessing.value = true

            // 2. Build full context (System Prompt + Workout Data + Chat History)
            val sets = repository.getAllSets().first()
            val baseMessages = AiPromptBuilder.buildSummaryPrompt(sets, language)
            val fullHistory = baseMessages + _chatMessages.value

            // 3. Call AI
            val result = aiRepository.getChatCompletion(baseUrl, apiKey, model, fullHistory)
            
            result.onSuccess {
                _chatMessages.value = _chatMessages.value + AiChatMessage("assistant", it)
            }.onFailure {
                _chatMessages.value = _chatMessages.value + AiChatMessage("assistant", "Error: ${it.message}")
            }
            _isChatProcessing.value = false
        }
    }
}
