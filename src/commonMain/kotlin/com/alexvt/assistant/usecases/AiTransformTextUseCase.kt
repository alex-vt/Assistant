package com.alexvt.assistant.usecases

import com.alexvt.assistant.repository.AiTransformTextRepository
import com.alexvt.assistant.repository.AiTransformTextRepository.Response
import java.math.MathContext

class AiTransformTextUseCase(
    private val repository: AiTransformTextRepository,
) {

    data class TextTransformationResult(
        val isDryRun: Boolean,
        val resultText: String,
        val estimatedCost: ComputeCost,
        val actualCost: ComputeCost,
    )

    data class ComputeCost(
        val computeRounds: List<ComputeRound>,
        val usd: Double,
        val text: String,
    )

    data class ComputeRound(
        val isReducedComplexity: Boolean,
        val computeUnits: Int,
        val usd: Double,
    )

    suspend fun execute(
        text: String,
        isDryRun: Boolean,
        isReducedComplexity: Boolean = false,
    ): TextTransformationResult {
        val simulatedResponse = getSimulatedMaxSizeResponse(text, isReducedComplexity)
        val response =
            if (isDryRun) {
                simulatedResponse
            } else {
                repository.getTransformed(text, isReducedComplexity)
            }
        return TextTransformationResult(
            isDryRun,
            resultText = response.text,
            estimatedCost = simulatedResponse.computeCost(),
            actualCost = if (isDryRun) zeroCost else response.computeCost()
        )
    }

    private fun Response.computeCost(): ComputeCost {
        val computeRounds = listOf(
            ComputeRound(
                isReducedComplexity,
                computeUnits = computeUnits,
                usd = computeUnits * repository.getComputeUnitCost(isReducedComplexity)
            )
        )
        return ComputeCost(
            computeRounds,
            usd = computeRounds.sumOf { it.usd },
            text = computeRounds.toEstimateText()
        )
    }

    private fun List<ComputeRound>.toEstimateText(): String {
        //if (size <= 1) return "" // considered less significant
        return "$size rounds, \$${sumOf { it.usd }.withDecimalPlaces(2)}"
    }

    private fun Double.withDecimalPlaces(places: Int): String =
        toBigDecimal().round(MathContext(places)).toString()

    private val zeroCost = ComputeCost(computeRounds = emptyList(), usd = 0.0, text = "")

    private fun getSimulatedMaxSizeResponse(
        text: String,
        isReducedComplexity: Boolean
    ): Response =
        Response(
            text = "[simulated response]",
            computeUnits = repository.getComputeUnitsEstimate(text, isReducedComplexity),
            isReducedComplexity
        )

}
