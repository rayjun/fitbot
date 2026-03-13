package com.fitness.util

import com.fitness.model.AiChatMessage
import com.fitness.model.ExerciseSet
import com.fitness.data.AnalyticsEngine

object AiPromptBuilder {
    
    private const val SYSTEM_PROMPT = """
        You are a professional fitness AI coach. Analyze the user's workout data and provide a concise, encouraging, and actionable summary.
        Rules:
        1. Only discuss the provided fitness data.
        2. Be brief (max 150 words).
        3. Identify one strength and one area for improvement.
        4. If the data is empty, encourage the user to start their first workout.
        5. Respond in the same language as the user's request (English or Chinese).
    """

    fun buildSummaryPrompt(sets: List<ExerciseSet>, language: String): List<AiChatMessage> {
        val volumeData = AnalyticsEngine.calculateVolumePerMuscleGroup(sets)
        val statsContext = volumeData.entries.joinToString("\n") { 
            "- ${it.key}: ${it.value.toInt()} kg total volume" 
        }
        
        val userMessage = if (language == "zh") {
            "这是我最近的训练数据概要：\n$statsContext\n请分析我的训练表现并给出建议。"
        } else {
            "Here is a summary of my recent workout data:\n$statsContext\nPlease analyze my performance and provide suggestions."
        }

        return listOf(
            AiChatMessage("system", SYSTEM_PROMPT),
            AiChatMessage("user", userMessage)
        )
    }
}
