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
        instructionLanguageModel: String = "DaVinci",
        shorteningInstruction: String = "\n\nThe text above, slightly shortened:",
        shorteningLanguageModel: String = "Turbo",
        isDryRun: Boolean,
    ): TextTransformationResult {
        val availableLanguageModels = repositories.map { it.getLanguageModel() }
        require(instructionLanguageModel in availableLanguageModels) {
            "Unknown language model: $instructionLanguageModel, available: $availableLanguageModels"
        }
        require(shorteningLanguageModel in availableLanguageModels) {
            "Unknown language model: $instructionLanguageModel, available: $availableLanguageModels"
        }
        val instructionRepository =
            repositories.first { it.getLanguageModel() == instructionLanguageModel }
        val shorteningRepository =
            repositories.first { it.getLanguageModel() == shorteningLanguageModel }
        val simulatedResponses = getResponsesForParts(
            text, postfixInstruction, instructionRepository,
            shorteningInstruction, shorteningRepository,
            isDryRun = true,
        )
        val executionResponses = getResponsesForParts(
            text, postfixInstruction, instructionRepository,
            shorteningInstruction, shorteningRepository,
            isDryRun,
        )
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
     * Each part will be maxed out to be transformed with a shortening instruction in 1 round.
     *
     * 3. The multiple part results will be concatenated in a new text; then going back to step 1.
     */
    private suspend fun getResponsesForParts(
        textWithoutInstruction: String,
        postfixInstruction: String,
        instructionRepository: AiTextRepository,
        shorteningInstruction: String,
        shorteningRepository: AiTextRepository,
        isDryRun: Boolean,
    ): List<Response> {
        val isOneRoundRemaining =
            instructionRepository.getComputeUnitsTotalEstimate(
                inputText = textWithoutInstruction + postfixInstruction
            ) <= instructionRepository.getComputeUnitsTotalLimit()
        if (isOneRoundRemaining) {
            return getSingleRoundResponse(
                inputText = textWithoutInstruction + postfixInstruction,
                repository = instructionRepository,
                isDryRun,
            ).run(::listOf)
        }
        val textPartsToShorten =
            textWithoutInstruction.splitIntoOverlappingParts(shorteningRepository)
        val shortenedTextPartResponses = textPartsToShorten.map { textPartWithoutInstruction ->
            getSingleRoundResponse(
                inputText = textPartWithoutInstruction + shorteningInstruction,
                repository = shorteningRepository,
                isDryRun,
            )
        }
        return shortenedTextPartResponses + getResponsesForParts(
            textWithoutInstruction = shortenedTextPartResponses.joinToString(separator = "\n\n") {
                it.text
            },
            postfixInstruction, instructionRepository,
            shorteningInstruction, shorteningRepository,
            isDryRun,
        )
    }

    /**
     * In a long text, the beginning of maximum possible size is repeatedly cut off.
     * The remaining part is backtracked by the size of overlap.
     * Max possible size (in characters) for each part is the one which is tokenized into
     * no more than max tokens for the given language model.
     */
    private fun String.splitIntoOverlappingParts(
        shorteningRepository: AiTextRepository,
        overlapByCharacters: Int = 100, // to minimize context loss at between-parts boundaries
        splittingAccuracy: Int = 10, // how close (or closer) is enough to approach max part size
    ): List<String> {
        val resultingParts = mutableListOf<String>()
        var remainingBigText = this
        while (remainingBigText.isNotEmpty()) {
            // Because each token typically corresponds to one or a few characters,
            // part size search (in characters) increment starts with token limit for it,
            // with exponential halving when gradually meeting the target size.
            // This way the big text is split into minimal possible number of parts.
            var currentPartSizeIncrement = with(shorteningRepository) {
                getComputeUnitsTotalLimit() - getComputeUnitsResponseLimit()
            }
            var currentPartSize = 0
            while (currentPartSizeIncrement > splittingAccuracy) {
                while (
                    canIncreasePart(
                        remainingBigText,
                        currentPartSize,
                        proposedPartSizeIncrement = currentPartSizeIncrement,
                        shorteningRepository
                    )
                ) {
                    currentPartSize += currentPartSizeIncrement
                }
                currentPartSizeIncrement /= 2
            }
            val currentPart = remainingBigText.take(currentPartSize)
            // if end of big text reached, no need to backtrack by overlap
            remainingBigText =
                remainingBigText.drop(currentPartSize).takeIf { it.isEmpty() }
                    ?: remainingBigText.drop(
                        (currentPartSize - overlapByCharacters).coerceAtLeast(0)
                    )
            resultingParts.add(currentPart)
        }
        return resultingParts
    }

    private fun canIncreasePart(
        remainingBigText: String,
        currentPartSize: Int,
        proposedPartSizeIncrement: Int,
        shorteningRepository: AiTextRepository,
    ): Boolean {
        if (remainingBigText.length <= currentPartSize) return false // no room to increase part
        val proposedPart = remainingBigText.take(currentPartSize + proposedPartSizeIncrement)
        return with(shorteningRepository) {
            getComputeUnitsTotalEstimate(proposedPart) <= getComputeUnitsTotalLimit()
        }
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
                computeUnits = computeUnitsTotal,
                usd = computeUnitsTotal * computeUnitCostUsd
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
        return "~ $size rounds, \$${sumOf { it.usd }.withDecimalPlaces(2)}"
    }

    private fun Double.withDecimalPlaces(places: Int): String =
        toBigDecimal().round(MathContext(places)).toString()

    private val zeroCost = ComputeCost(computeRounds = emptyList(), usd = 0.0, text = "")

    private suspend fun getSingleRoundResponse(
        inputText: String,
        repository: AiTextRepository,
        isDryRun: Boolean,
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
            computeUnitsTotal = repository.getComputeUnitsTotalEstimate(inputText),
            computeUnitCostUsd = repository.getComputeUnitCostUsd(),
        )

}
