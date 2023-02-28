package com.alexvt.assistant.usecases

import com.alexvt.assistant.repository.AiTransformTextRepository

class AiTransformTextUseCase(
    private val repository: AiTransformTextRepository,
) {

    // todo implement
    data class TextTransformationResult(
        val isDryRun: Boolean,
        val resultText: String,
        val estimatedCost: ComputeCost,
        val actualCost: ComputeCost,
    )

    data class ComputeCost(
        val computeRounds: List<ComputeRound>,
        val timeMillis: Int,
        val usd: Double,
    )

    data class ComputeRound(
        val isReducedComplexity: Boolean,
        val dataSizePoints: Int,
        val errorMessageOrNull: String?,
        val timeMillis: Int,
        val usd: Double,
    )

    suspend fun execute(text: String): String {
        return repository.getTransformed(text)
    }

}
