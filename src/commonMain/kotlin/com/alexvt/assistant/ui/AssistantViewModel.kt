package com.alexvt.assistant.ui

import androidx.compose.ui.geometry.Offset
import com.alexvt.assistant.usecases.AiTranscribeFromMicStopRecordingUseCase
import com.alexvt.assistant.usecases.AiTranscribeFromMicUseCase
import com.alexvt.assistant.usecases.AiTransformTextUseCase
import com.alexvt.assistant.usecases.ExtractTextFromImageUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import kotlin.math.max
import kotlin.math.min

class AssistantViewModel constructor(
    private val mainThreadCoroutineScope: CoroutineScope,
    private val backgroundCoroutineScope: CoroutineScope,
    private val aiTransformTextUseCase: AiTransformTextUseCase,
    private val extractTextFromImageUseCase: ExtractTextFromImageUseCase,
    private val aiTranscribeFromMicUseCase: AiTranscribeFromMicUseCase,
    private val aiTranscribeFromMicStopRecordingUseCase: AiTranscribeFromMicStopRecordingUseCase,
) {

    private val uiStateFlow: MutableStateFlow<UiState> = MutableStateFlow(initialUiState)
    private val uiEventFlow: MutableSharedFlow<UiEvent> = MutableSharedFlow()

    fun getUiStateFlow(): StateFlow<UiState> =
        uiStateFlow

    fun getUiEventFlow(): SharedFlow<UiEvent> =
        uiEventFlow

    data class UiState(
        val isActive: Boolean,
        val isInstructionLanguageModelSelected: Boolean,
        val instructionLanguageModel: String,
        val actionButtonNames: List<String>,
        val actionIconTints: List<Int>,
        val actionButtonSelectedIndex: Int,
        val isBusyGettingResponse: Boolean,
        val text: String,
        val estimateText: String,
        val actualCostText: String,
        val isPreviewingScreenshot: Boolean,
        val isPickingScreenshot: Boolean,
        val screenshotStartX: Int,
        val screenshotStartY: Int,
        val screenshotRectTop: Int,
        val screenshotRectBottom: Int,
        val screenshotRectLeft: Int,
        val screenshotRectRight: Int,
        val isBusyGettingTextFromScreenshot: Boolean,
        val isRecordingFromMic: Boolean,
        val isBusyGettingTextFromMicRecording: Boolean,
    )

    private sealed class TextAction
    private data class SearchOnlineTextAction(
        val aiPostfixInstruction: String, // used to make a search request with an language model
        val prefixUrl: String,
    ) : TextAction()

    private data class AiCompleteTextAction(
        val postfixInstruction: String,
        val isResultSeparated: Boolean,
    ) : TextAction()

    sealed class UiEvent
    data class ViewExternally(val url: String) : UiEvent()
    data class TextGenerated(val text: String) : UiEvent()

    private data class ActionButtonModel(
        val title: String,
        val iconTint: Int,
        val textAction: TextAction
    )

    fun clear() {
        uiStateFlow.value = uiStateFlow.value.copy(text = "")
        mainThreadCoroutineScope.launch {
            uiEventFlow.emit(TextGenerated(""))
        }
    }

    fun onTextChanged(newText: String) {
        if (newText != uiStateFlow.value.text) {
            uiStateFlow.value = uiStateFlow.value.copy(
                text = newText,
                actualCostText = "",
            )
            tryRunSelectedAction(isExplicitRunCommandPassed = false)
        }
    }

    fun onEscape() {
        if (uiStateFlow.value.isPickingScreenshot) {
            uiStateFlow.value = uiStateFlow.value.copy(
                isPickingScreenshot = false
            )
        } else {
            uiStateFlow.value = uiStateFlow.value.copy(
                isActive = false
            )
        }
    }

    fun onMicButton() {
        val isRecording = uiStateFlow.value.isRecordingFromMic
        if (isRecording) {
            aiTranscribeFromMicStopRecordingUseCase.execute()
            uiStateFlow.value = uiStateFlow.value.copy(
                isRecordingFromMic = false,
                isBusyGettingTextFromMicRecording = true,
            )
        } else {
            uiStateFlow.value = uiStateFlow.value.copy(
                isRecordingFromMic = true,
            )
            backgroundCoroutineScope.launch {
                val transcriptionResponse = aiTranscribeFromMicUseCase.execute()
                val textBeforeTranscription = uiStateFlow.value.text
                val textAfterTranscription =
                    textBeforeTranscription.extendedWith(transcriptionResponse.text)
                mainThreadCoroutineScope.launch {
                    uiEventFlow.emit(TextGenerated(textAfterTranscription))
                    uiStateFlow.value = uiStateFlow.value.copy(
                        isRecordingFromMic = false,
                        isBusyGettingTextFromMicRecording = false,
                        text = textAfterTranscription,
                    )
                }
            }
        }
    }

    fun previewTakeScreenshot() {
        uiStateFlow.value = uiStateFlow.value.copy(
            isPreviewingScreenshot = true,
        )
    }

    fun onFreeAreaPointerDown(x: Int, y: Int) {
        if (uiStateFlow.value.isBusyGettingTextFromScreenshot) return
        if (uiStateFlow.value.isBusyGettingResponse) return
        if (uiStateFlow.value.isPickingScreenshot) return
        if (!uiStateFlow.value.isPreviewingScreenshot) return
        uiStateFlow.value = uiStateFlow.value.copy(
            isPreviewingScreenshot = false,
            isPickingScreenshot = true,
            screenshotStartX = x,
            screenshotStartY = y,
            screenshotRectTop = y,
            screenshotRectBottom = y,
            screenshotRectLeft = x,
            screenshotRectRight = x,
        )
    }

    fun onFreeAreaPointerMove(x: Int, y: Int) {
        if (!uiStateFlow.value.isPickingScreenshot) return
        uiStateFlow.value = uiStateFlow.value.copy(
            screenshotRectTop = min(uiStateFlow.value.screenshotStartY, y),
            screenshotRectBottom = max(uiStateFlow.value.screenshotStartY, y),
            screenshotRectLeft = min(uiStateFlow.value.screenshotStartX, x),
            screenshotRectRight = max(uiStateFlow.value.screenshotStartX, x),
        )
    }

    fun onFreeAreaPointerRelease(x: Int, y: Int, windowOffset: Offset) {
        if (!uiStateFlow.value.isPickingScreenshot) return
        uiStateFlow.value = uiStateFlow.value.copy(
            isPickingScreenshot = false,
            isBusyGettingTextFromScreenshot = true,
            screenshotRectTop = min(uiStateFlow.value.screenshotStartY, y),
            screenshotRectBottom = max(uiStateFlow.value.screenshotStartY, y),
            screenshotRectLeft = min(uiStateFlow.value.screenshotStartX, x),
            screenshotRectRight = max(uiStateFlow.value.screenshotStartX, x),
        )
        backgroundCoroutineScope.launch {
            val textBeforeAction = uiStateFlow.value.text
            val textAfterAction = textBeforeAction.extendedWith(
                // screenshot coordinates are absolute: sum of local ones and window offset
                appendedText = extractTextFromImageUseCase.execute(
                    uiStateFlow.value.screenshotRectTop + windowOffset.y.toInt(),
                    uiStateFlow.value.screenshotRectBottom + windowOffset.y.toInt(),
                    uiStateFlow.value.screenshotRectLeft + windowOffset.x.toInt(),
                    uiStateFlow.value.screenshotRectRight + windowOffset.x.toInt(),
                )
            )
            mainThreadCoroutineScope.launch {
                uiEventFlow.emit(TextGenerated(textAfterAction))
                uiStateFlow.value = uiStateFlow.value.copy(
                    isBusyGettingTextFromScreenshot = false,
                    text = textAfterAction
                )
            }
        }
    }

    private fun getSelectedTextAction(): TextAction =
        actionButtonModels[uiStateFlow.value.actionButtonSelectedIndex].textAction

    private var actionRunJobOrNull: Job? = null

    /**
     * Depending on state of availability, run command and language model selection,
     * this may be an actual transformation run, a dry run (cost estimate), or no operation.
     */
    private fun tryRunSelectedAction(isExplicitRunCommandPassed: Boolean) {
        with(uiStateFlow.value) {
            if (!isActive) return
            if (isBusyGettingResponse) return
            if (isBusyGettingTextFromScreenshot) return
            if (isBusyGettingTextFromMicRecording) return
        }
        with(getSelectedTextAction()) {
            val isActualRun =
                isExplicitRunCommandPassed && uiStateFlow.value.isInstructionLanguageModelSelected
            uiStateFlow.value = uiStateFlow.value.copy(
                estimateText = if (isActualRun) "" else "computing cost ...",
                isBusyGettingResponse = isActualRun,
            )
            actionRunJobOrNull?.cancel()
            actionRunJobOrNull = backgroundCoroutineScope.launch {
                val textBeforeAction = uiStateFlow.value.text
                val runResult = aiTransformTextUseCase.execute(
                    text = textBeforeAction,
                    instructionLanguageModel = uiStateFlow.value.instructionLanguageModel,
                    postfixInstruction = when (this@with) {
                        is AiCompleteTextAction -> postfixInstruction
                        is SearchOnlineTextAction -> aiPostfixInstruction
                    },
                    isDryRun = !isActualRun,
                )
                val textAfterAction = if (isActualRun) {
                    when (this@with) {
                        is AiCompleteTextAction -> textBeforeAction.extendedWith(
                            appendedText = runResult.resultText,
                            isAppendedTextSeparated = isResultSeparated
                        )

                        is SearchOnlineTextAction -> prefixUrl + withContext(Dispatchers.IO) {
                            URLEncoder.encode(runResult.resultText, "utf-8")
                        }
                    }
                } else {
                    textBeforeAction
                }
                if (!isActive) return@launch
                mainThreadCoroutineScope.launch {
                    uiEventFlow.emit(TextGenerated(textAfterAction))
                    if (this@with is SearchOnlineTextAction) {
                        uiEventFlow.emit(ViewExternally(textAfterAction))
                    }
                    uiStateFlow.value = uiStateFlow.value.copy(
                        isActive = this@with !is SearchOnlineTextAction,
                        isBusyGettingResponse = false,
                        text = textAfterAction,
                        estimateText = "~ ${runResult.estimatedCost.text}",
                        actualCostText = if (isActualRun) {
                            "used: ${runResult.actualCost.text}"
                        } else {
                            ""
                        },
                    )
                }
            }
        }
    }

    fun onInputEnter() {
        tryRunSelectedAction(isExplicitRunCommandPassed = true)
    }

    private fun String.extendedWith(
        appendedText: String,
        isAppendedTextSeparated: Boolean = true
    ): String {
        val newTextSeparator =
            if (isAppendedTextSeparated) paragraphSeparator else emptyText
        return (trimEndIf(isAppendedTextSeparated) + newTextSeparator +
                appendedText.trimStartIf(isAppendedTextSeparated) + paragraphSeparator)
            .trimStart().ifBlank { emptyText }
    }

    private fun String.trimStartIf(condition: Boolean): String =
        if (condition) trimStart() else this

    private fun String.trimEndIf(condition: Boolean): String =
        if (condition) trimEnd() else this

    fun onActionButtonClick(buttonIndex: Int) {
        uiStateFlow.value = uiStateFlow.value.copy(
            actionButtonSelectedIndex = buttonIndex
        )
        tryRunSelectedAction(isExplicitRunCommandPassed = true)
    }

    fun onInstructionModelSelectMin() {
        uiStateFlow.value = uiStateFlow.value.copy(
            isInstructionLanguageModelSelected = true,
            instructionLanguageModel = "Turbo",
        )
        tryRunSelectedAction(isExplicitRunCommandPassed = false)
    }

    fun onInstructionModelSelectMedium() {
        uiStateFlow.value = uiStateFlow.value.copy(
            isInstructionLanguageModelSelected = true,
            instructionLanguageModel = "DaVinci",
        )
        tryRunSelectedAction(isExplicitRunCommandPassed = false)
    }

    fun onInstructionModelSelectMax() {
        uiStateFlow.value = uiStateFlow.value.copy(
            isInstructionLanguageModelSelected = true,
            instructionLanguageModel = "GPT4",
        )
        tryRunSelectedAction(isExplicitRunCommandPassed = false)
    }

    fun onInstructionModelUnselect() {
        uiStateFlow.value = uiStateFlow.value.copy(
            isInstructionLanguageModelSelected = false,
            instructionLanguageModel = "Turbo", // for by-default cost estimates
        )
        tryRunSelectedAction(isExplicitRunCommandPassed = false)
    }

    companion object {
        private const val paragraphSeparator = "\n\n"
        private const val emptyText = ""

        private val actionButtonModels = listOf(
            ActionButtonModel(
                "YouTube", 0xFF4444,
                SearchOnlineTextAction(
                    aiPostfixInstruction = "\n\nMake a good search phrase for the text above:\n",
                    prefixUrl = "https://www.youtube.com/results?search_query=",
                )
            ),
            ActionButtonModel(
                "W|Alpha", 0xFF7700,
                SearchOnlineTextAction(
                    aiPostfixInstruction = "\n\nMake a good search phrase for the text above:\n",
                    prefixUrl = "https://www.wolframalpha.com/input?i=",
                )
            ),
            ActionButtonModel(
                "Google", 0x77AAFF,
                SearchOnlineTextAction(
                    aiPostfixInstruction = "\n\nMake a good search phrase for the text above:\n",
                    prefixUrl = "https://www.google.com/search?q=",
                )
            ),
            ActionButtonModel(
                "Answer", 0x44AA77,
                AiCompleteTextAction(
                    postfixInstruction = "\n\nAnswer to the text above:\n",
                    isResultSeparated = true
                )
            ),
            ActionButtonModel(
                "Summary", 0xFFFF00,
                AiCompleteTextAction(
                    postfixInstruction = "\n\nSummary of the main part of the text above, in bullet points:\n",
                    isResultSeparated = true
                )
            ),
            ActionButtonModel(
                "Rewrite", 0xAAAAFF,
                AiCompleteTextAction(
                    postfixInstruction = "\n\nThe text above, rephrased:\n",
                    isResultSeparated = true
                )
            ),
            ActionButtonModel(
                "Continue", 0xAAAAAA,
                AiCompleteTextAction(
                    postfixInstruction = "",
                    isResultSeparated = false
                )
            ),
        )
        private const val defaultSelectedActionIndex = 3
        private val initialUiState = UiState(
            isActive = true,
            instructionLanguageModel = "Turbo",
            isInstructionLanguageModelSelected = false,
            actionButtonNames = actionButtonModels.map { it.title },
            actionIconTints = actionButtonModels.map { it.iconTint },
            actionButtonSelectedIndex = defaultSelectedActionIndex,
            isBusyGettingResponse = false,
            text = "",
            estimateText = "",
            actualCostText = "",
            isPreviewingScreenshot = false,
            isPickingScreenshot = false,
            screenshotStartX = 0,
            screenshotStartY = 0,
            screenshotRectTop = 0,
            screenshotRectBottom = 0,
            screenshotRectLeft = 0,
            screenshotRectRight = 0,
            isBusyGettingTextFromScreenshot = false,
            isRecordingFromMic = false,
            isBusyGettingTextFromMicRecording = false,
        )
    }
}
