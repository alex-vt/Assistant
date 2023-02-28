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

class AiTransformTextRepository(private val credentialsRepository: CredentialsRepository) {

    suspend fun getTransformed(
        text: String,
        normalizedRandomness: Double = 0.35,
        isReducedComplexity: Boolean = false
    ): String {
        return HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 60_000
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
                    model = if (isReducedComplexity) "text-curie-001" else "text-davinci-003",
                    text,
                    max_tokens = 512,
                    temperature = normalizedRandomness.denormalizedToTemperature(maxValue = 2.0)
                )
            )
        }.body<OpenAiCompletionResponseV1>().choices.joinToString { it.text }
    }

    private fun Double.denormalizedToTemperature(maxValue: Double) =
        this * maxValue

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

    private companion object {
        private const val TOKEN_LIMIT_4096 = 4096
        private const val USD_PER_TOKEN_DAVINCI = 0.00002
        private const val USD_PER_TOKEN_CURIE = 0.000002
    }
}
