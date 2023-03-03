package com.alexvt.assistant.repository

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class AiTextChatRepository(private val credentialsRepository: CredentialsRepository) :
    AiTextRepository() {

    override val model = LanguageModel(
        name = "gpt-3.5-turbo-0301",
        label = "Turbo",
        maxTotalTokens = 4096,
        maxResponseTokens = 512,
        usdPerToken = 0.000002,
        timeoutMillis = 20_000,
    )

    override suspend fun getTransformedWithTemperature(
        inputText: String,
        temperature: Double
    ): Response {
        val apiResponse = HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = model.timeoutMillis
            }
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                })
            }
        }.apply {
            plugin(HttpSend).intercept { request ->
                println(request.body.toString())
                val response = execute(request) // todo log
                response
            }
        }.post("https://api.openai.com/v1/chat/completions") {
            headers {
                append("Content-Type", "application/json")
                append("Authorization", "Bearer ${credentialsRepository.getOpenAiBearerToken()}")
            }
            setBody(
                OpenAiChatRequestV1(
                    model = model.name,
                    messages = listOf(OpenAiChatMessageV1(role = "user", content = inputText)),
                    max_tokens = model.maxResponseTokens,
                    temperature,
                )
            )
        }.body<OpenAiChatResponseV1>()
        return Response(
            text = apiResponse.choices.joinToString { it.message.content },
            languageModel = model.label,
            computeUnits = apiResponse.usage.total_tokens,
            computeUnitCost = model.usdPerToken,
        )
    }

    @Serializable
    private data class OpenAiChatResponseV1(
        val id: String,
        val `object`: String,
        val created: Long,
        val model: String,
        val choices: List<OpenAiChatResponseChoice>,
        val usage: OpenAiResponseUsage,
    )

    @Serializable
    private data class OpenAiChatResponseChoice(
        val index: Int,
        val message: OpenAiChatMessageV1,
        val finish_reason: String?,
    )

    @Serializable
    private data class OpenAiResponseUsage(
        val prompt_tokens: Int,
        val completion_tokens: Int?,
        val total_tokens: Int,
    )

    @Serializable
    private data class OpenAiChatRequestV1(
        val model: String,
        val messages: List<OpenAiChatMessageV1>,
        val max_tokens: Int,
        val temperature: Double,
    )

    @Serializable
    private data class OpenAiChatMessageV1(
        val role: String, val content: String
    )

}
