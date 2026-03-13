package com.fitness.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AiChatMessage(
    val role: String,
    val content: String
)

@Serializable
data class AiChatRequest(
    val model: String,
    val messages: List<AiChatMessage>,
    val stream: Boolean = false,
    val temperature: Double = 0.7
)

@Serializable
data class AiChatChoice(
    val message: AiChatMessage,
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
data class AiChatResponse(
    val id: String? = null,
    val choices: List<AiChatChoice>
)
