package com.alexvt.assistant.repository

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
     * A conservative estimate is 1 token for 1 character.
     * todo use tokenizer for estimation
     */
    fun getComputeUnitsTotalEstimate(inputText: String): Int =
        inputText.length + model.maxResponseTokens

    fun getTextSizeForComputeUnits(computeUnits: Int): Int =
        computeUnits // same conservative estimate

    fun getComputeUnitsTotalLimit(): Int =
        model.maxTotalTokens

    fun getComputeUnitsResponseLimit(): Int =
        model.maxResponseTokens

    fun getComputeUnitCost(): Double =
        model.usdPerToken

    fun getLanguageModel(): String =
        model.label

    data class Response(
        val text: String,
        val languageModel: String,
        val computeUnits: Int,
        val computeUnitCost: Double,
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
