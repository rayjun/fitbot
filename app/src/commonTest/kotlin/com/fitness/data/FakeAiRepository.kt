package com.fitness.data

import com.fitness.model.AiChatMessage

class FakeAiRepository : AiRepository {
    var nextResult: Result<String> = Result.success("Mock AI Response")

    override suspend fun getChatCompletion(
        baseUrl: String,
        apiKey: String,
        model: String,
        messages: List<AiChatMessage>
    ): Result<String> {
        return nextResult
    }
}
