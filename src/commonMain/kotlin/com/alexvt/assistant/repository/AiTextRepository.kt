package com.alexvt.assistant.repository

import com.knuddels.jtokkit.Encodings
import com.knuddels.jtokkit.api.ModelType

abstract class AiTextRepository {

    protected data class LanguageModel(
        val label: String,
        val name: String,
        val maxTemperature: Double = 2.0,
        val maxTotalTokens: Int,
        val maxResponseTokens: Int,
        val usdPerToken: Double,
        val timeoutMillis: Long,
    )

    protected abstract val model: LanguageModel

    /**
     * A compute unit for OpenAI API is a token.
     * While response size in tokens is unknown and estimated at maximum,
     * request size in tokens is calculated and added to response size.
     */
    fun getComputeUnitsTotalEstimate(inputText: String): Int =
        getComputeUnitsRequestEstimate(inputText) + model.maxResponseTokens

    private fun getComputeUnitsRequestEstimate(requestText: String): Int {
        return Encodings.newDefaultEncodingRegistry()
            .getEncodingForModel(
                ModelType.valueOf(
                    model.name.uppercase().replace('-', '_').replace('.', '_')
                )
            )
            .countTokens(requestText)
    }

    fun getComputeUnitsTotalLimit(): Int =
        model.maxTotalTokens

    fun getComputeUnitsResponseLimit(): Int =
        model.maxResponseTokens

    fun getComputeUnitCostUsd(): Double =
        model.usdPerToken

    fun getLanguageModel(): String =
        model.label

    data class Response(
        val text: String,
        val languageModel: String,
        val computeUnitsTotal: Int,
        val computeUnitCostUsd: Double,
    )

    suspend fun getTransformed(inputText: String, normalizedRandomness: Double = 0.35): Response =
        getTransformedWithTemperature(
            inputText,
            temperature = model.maxTemperature * normalizedRandomness,
        )

    protected abstract suspend fun getTransformedWithTemperature(
        inputText: String,
        temperature: Double
    ): Response

}