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

abstract class AiTextCompleteRepository(private val credentialsRepository: CredentialsRepository) :
    AiTextRepository() {

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
        }.post("https://api.openai.com/v1/completions") {
            headers {
                append("Content-Type", "application/json")
                append("Authorization", "Bearer ${credentialsRepository.getOpenAiBearerToken()}")
            }
            setBody(
                OpenAiCompletionRequestV1(
                    model = model.name,
                    inputText,
                    max_tokens = model.maxResponseTokens,
                    temperature,
                )
            )
        }.body<OpenAiCompletionResponseV1>()
        return Response(
            text = apiResponse.choices.joinToString { it.text },
            languageModel = model.label,
            computeUnits = apiResponse.usage.total_tokens,
            computeUnitCost = model.usdPerToken,
        )
    }

    @Serializable
    private data class OpenAiCompletionResponseV1(
        val id: String,
        val `object`: String,
        val created: Long,
        val model: String,
        val choices: List<OpenAiCompletionResponseChoice>,
        val usage: OpenAiResponseUsage,
    )

    @Serializable
    private data class OpenAiCompletionResponseChoice(
        val text: String,
        val index: Int,
        val logprobs: Int?,
        val finish_reason: String?,
    )

    @Serializable
    private data class OpenAiResponseUsage(
        val prompt_tokens: Int,
        val completion_tokens: Int?,
        val total_tokens: Int,
    )

    @Serializable
    private data class OpenAiCompletionRequestV1(
        val model: String, val prompt: String, val max_tokens: Int, val temperature: Double
    )

}
