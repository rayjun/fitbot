package com.fitness.data

import com.fitness.model.AiChatMessage
import com.fitness.model.AiChatRequest
import com.fitness.model.AiChatResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

interface AiRepository {
    suspend fun getChatCompletion(
        baseUrl: String,
        apiKey: String,
        model: String,
        messages: List<AiChatMessage>
    ): Result<String>
}

class KtorAiRepository(
    private val client: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
) : AiRepository {
    override suspend fun getChatCompletion(
        baseUrl: String,
        apiKey: String,
        model: String,
        messages: List<AiChatMessage>
    ): Result<String> {
        if (apiKey.isEmpty()) return Result.failure(Exception("API Key is empty"))
        
        val url = if (baseUrl.endsWith("/")) "${baseUrl}chat/completions" else "$baseUrl/chat/completions"
        
        return try {
            val response: AiChatResponse = client.post(url) {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(AiChatRequest(model = model, messages = messages))
            }.body()
            
            val content = response.choices.firstOrNull()?.message?.content
            if (content != null) {
                Result.success(content)
            } else {
                Result.failure(Exception("Empty response from AI"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
