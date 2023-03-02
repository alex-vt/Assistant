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

    private data class LanguageModel(
        val name: String,
        val maxTotalTokens: Int,
        val maxResponseTokens: Int,
        val usdPerToken: Double,
        val timeoutMillis: Long,
    )

    private val curieModel = LanguageModel(
        name = "text-curie-001",
        maxTotalTokens = 2048,
        maxResponseTokens = 256,
        usdPerToken = 0.000002,
        timeoutMillis = 20_000,
    )

    private val daVinciModel = LanguageModel(
        name = "text-davinci-003",
        maxTotalTokens = 4096,
        maxResponseTokens = 512,
        usdPerToken = 0.00002,
        timeoutMillis = 60_000,
    )

    private fun getModelForComplexity(isReducedComplexity: Boolean) =
        if (isReducedComplexity) curieModel else daVinciModel

    /**
     * A compute unit for OpenAI API is a token.
     * A conservative estimate is 1 token for 1 character.
     * todo use tokenizer for estimation
     */
    fun getComputeUnitsTotalEstimate(inputText: String, isReducedComplexity: Boolean): Int =
        inputText.length + getModelForComplexity(isReducedComplexity).maxResponseTokens

    fun getTextSizeForComputeUnits(computeUnits: Int): Int =
        computeUnits // same conservative estimate

    fun getComputeUnitsTotalLimit(isReducedComplexity: Boolean): Int =
        getModelForComplexity(isReducedComplexity).maxTotalTokens

    fun getComputeUnitsResponseLimit(isReducedComplexity: Boolean): Int =
        getModelForComplexity(isReducedComplexity).maxResponseTokens

    fun getComputeUnitCost(isReducedComplexity: Boolean): Double =
        getModelForComplexity(isReducedComplexity).usdPerToken

    data class Response(val text: String, val computeUnits: Int, val isReducedComplexity: Boolean)

    suspend fun getTransformed(
        inputText: String,
        isReducedComplexity: Boolean,
        normalizedRandomness: Double = 0.35
    ): Response {
        val model = getModelForComplexity(isReducedComplexity)
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
                    temperature = normalizedRandomness.denormalizedToTemperature(maxValue = 2.0)
                )
            )
        }.body<OpenAiCompletionResponseV1>()
        return Response(
            text = apiResponse.choices.joinToString { it.text },
            computeUnits = apiResponse.usage.total_tokens,
            isReducedComplexity
        )
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

}
