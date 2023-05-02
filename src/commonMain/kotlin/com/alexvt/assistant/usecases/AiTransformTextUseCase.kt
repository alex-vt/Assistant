package com.alexvt.assistant.usecases

import com.alexvt.assistant.AppScope
import com.alexvt.assistant.repository.AiTextChatGpt4Repository
import com.alexvt.assistant.repository.AiTextChatGptTurboRepository
import com.alexvt.assistant.repository.AiTextCompleteCurieRepository
import com.alexvt.assistant.repository.AiTextCompleteDaVinciRepository
import com.alexvt.assistant.repository.AiTextRepository
import com.alexvt.assistant.repository.AiTextRepository.Response
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.toList
import me.tatarka.inject.annotations.Inject
import java.math.MathContext

@AppScope
@Inject
class AiTransformTextUseCase(
    private val aiTextCurieRepository: AiTextCompleteCurieRepository,
    private val aiTextTurboRepository: AiTextChatGptTurboRepository,
    private val aiTextDaVinciRepository: AiTextCompleteDaVinciRepository,
    private val aiTextGpt4Repository: AiTextChatGpt4Repository,
) {

    data class TextTransformationResult(
        val isDryRun: Boolean,
        val resultText: String,
        val estimatedCost: ComputeCost,
        val actualCost: ComputeCost,
        val status: Status,
    )

    sealed class Status {
        object Success : Status()
        data class Error(val title: String, val details: String) : Status()
    }

    data class ComputeCost(
        val computeRounds: List<ComputeRound>,
        val usd: Double,
        val text: String,
    )

    data class ComputeRound(
        val languageModel: String,
        val computeUnitsInRequest: Int,
        val computeUnitsInResponse: Int,
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
        val aiTextRepositories = listOf(
            aiTextCurieRepository,
            aiTextTurboRepository,
            aiTextDaVinciRepository,
            aiTextGpt4Repository,
        )
        val availableLanguageModels = aiTextRepositories.map { it.getLanguageModel() }
        require(instructionLanguageModel in availableLanguageModels) {
            "Unknown language model: $instructionLanguageModel, available: $availableLanguageModels"
        }
        require(shorteningLanguageModel in availableLanguageModels) {
            "Unknown language model: $instructionLanguageModel, available: $availableLanguageModels"
        }
        val instructionRepository =
            aiTextRepositories.first { it.getLanguageModel() == instructionLanguageModel }
        val shorteningRepository =
            aiTextRepositories.first { it.getLanguageModel() == shorteningLanguageModel }
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
            actualCost = if (isDryRun) zeroCost else executionResponses.totalCost(),
            status = executionResponses.getStatus(),
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
        // todo implement retry
        // If an error occurs at one part shortening, next shortenings will be meaningless to do.
        // For a list of parts takeWhile { no error } after map { to shortened part }
        // will still shorten all parts and only afterwards start checking for errors.
        // The list is converted to sequence (flow) to avoid the shortening rounds after an error.
        val shortenedTextPartResponses = textPartsToShorten.asFlow() // no asSequence() for suspend
            .map { textPartWithoutInstruction ->
                getSingleRoundResponse(
                    inputText = textPartWithoutInstruction + shorteningInstruction,
                    repository = shorteningRepository,
                    isDryRun,
                )
            }.takeWhile { shortenedTextPartResponse ->
                shortenedTextPartResponse.errorTitle.isEmpty()
            }.toList()
        val isError = shortenedTextPartResponses.any { it.errorTitle.isNotEmpty() }
        if (isError) return shortenedTextPartResponses
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

    private fun List<Response>.getStatus(): Status =
        find { it.errorTitle.isNotEmpty() }?.run {
            Status.Error(title = errorTitle, details = errorText)
        } ?: Status.Success

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
                computeUnitsInRequest,
                computeUnitsInResponse,
                usd = computeUnitsInRequest * computeUnitRequestCostUsd
                        + computeUnitsInResponse * computeUnitResponseCostUsd
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
            computeUnitsInRequest = with(repository) {
                getComputeUnitsTotalEstimate(inputText) - getComputeUnitsResponseLimit()
            },
            computeUnitsInResponse = repository.getComputeUnitsResponseLimit(),
            computeUnitRequestCostUsd = repository.getComputeUnitRequestCostUsd(),
            computeUnitResponseCostUsd = repository.getComputeUnitResponseCostUsd(),
            errorTitle = "",
            errorText = "",
        )

}
