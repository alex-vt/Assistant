package com.alexvt.assistant.usecases

import com.alexvt.assistant.repository.AiTextRepository
import com.alexvt.assistant.repository.AiTextRepository.Response
import java.math.MathContext

class AiTransformTextUseCase(
    private val repositories: List<AiTextRepository>,
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
        val languageModel: String,
        val computeUnits: Int,
        val usd: Double,
    )

    suspend fun execute(
        text: String,
        postfixInstruction: String,
        isDryRun: Boolean,
        languageModel: String = "DaVinci",
    ): TextTransformationResult {
        val availableLanguageModels = repositories.map { it.getLanguageModel() }
        require(languageModel in availableLanguageModels) {
            "Unknown language model: $languageModel, available: $availableLanguageModels"
        }
        val repository = repositories.first { it.getLanguageModel() == languageModel }
        val simulatedResponses =
            getResponsesForParts(text, postfixInstruction, isDryRun = true, repository)
        val executionResponses =
            getResponsesForParts(text, postfixInstruction, isDryRun, repository)
        return TextTransformationResult(
            isDryRun,
            resultText = executionResponses.last().text, // the previous ones have partial results
            estimatedCost = simulatedResponses.totalCost(),
            actualCost = if (isDryRun) zeroCost else executionResponses.totalCost()
        )
    }

    /**
     * An arbitrarily sized input text, followed by an instruction of how to transform it,
     * is transformed in one or more rounds, depending on the text size.
     *
     * 1. If text and postfix instruction together don't exceed the maximum text size to transform,
     * their concatenation will be transformed in just 1 single round; done.
     *
     * 2. A bigger text will be cut in overlapping parts, to minimize context loss at the edges.
     * Each part will be maxed out to be transformed with a minimization instruction in 1 round.
     *
     * 3. The multiple part results will be concatenated in a new text; then going back to step 1.
     */
    private suspend fun getResponsesForParts(
        textWithoutInstruction: String,
        postfixInstruction: String,
        isDryRun: Boolean,
        repository: AiTextRepository,
    ): List<Response> {
        val isOneRoundRemaining =
            repository.getComputeUnitsTotalEstimate(
                inputText = textWithoutInstruction + postfixInstruction
            ) <= repository.getComputeUnitsTotalLimit()
        if (isOneRoundRemaining) {
            return getSingleRoundResponse(
                inputText = textWithoutInstruction + postfixInstruction,
                isDryRun,
                repository,
            ).run(::listOf)
        }
        val partMinimizationPostfixInstruction = "\n\nThe text above, slightly shortened:"
        val textPartSize = with(repository) {
            val textPartComputeCostBudget =
                getComputeUnitsTotalLimit() -
                        getComputeUnitsTotalEstimate(
                            inputText = partMinimizationPostfixInstruction
                        )
            getTextSizeForComputeUnits(textPartComputeCostBudget)
        } // todo max out each part with a tokenizer
        val relativeOverlapBetweenParts = 0.1
        val textParts = textWithoutInstruction.windowed(
            size = textPartSize,
            step = (textPartSize * (1 - relativeOverlapBetweenParts)).toInt(),
            partialWindows = true
        )
        val partResponses = textParts.map { textPartWithoutInstruction ->
            getSingleRoundResponse(
                inputText = textPartWithoutInstruction + partMinimizationPostfixInstruction,
                isDryRun,
                repository,
            )
        }
        return partResponses + getResponsesForParts(
            textWithoutInstruction = partResponses.joinToString(separator = "\n\n") { it.text },
            postfixInstruction,
            isDryRun,
            repository,
        )
    }

    private fun List<Response>.totalCost(): ComputeCost {
        val computeRounds = map { it.computeCost().computeRounds }.flatten()
        return ComputeCost(
            computeRounds,
            usd = sumOf { it.computeCost().usd },
            text = computeRounds.toEstimateText()
        )
    }

    private fun Response.computeCost(): ComputeCost {
        val computeRounds = listOf(
            ComputeRound(
                languageModel,
                computeUnits = computeUnits,
                usd = computeUnits * computeUnitCost
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

    private suspend fun getSingleRoundResponse(
        inputText: String,
        isDryRun: Boolean,
        repository: AiTextRepository,
    ): Response =
        if (isDryRun) {
            getSimulatedMaxSizeResponse(inputText, repository)
        } else {
            repository.getTransformed(inputText)
        }

    private fun getSimulatedMaxSizeResponse(
        inputText: String,
        repository: AiTextRepository,
    ): Response =
        Response(
            text = "#".repeat(repository.getComputeUnitsResponseLimit()),
            languageModel = repository.getLanguageModel(),
            computeUnits = repository.getComputeUnitsTotalEstimate(inputText),
            computeUnitCost = repository.getComputeUnitCost(),
        )

}
