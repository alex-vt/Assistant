package com.alexvt.assistant.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle.Companion.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alexvt.assistant.platform.openLinkInChrome
import com.alexvt.assistant.repository.*
import com.alexvt.assistant.uicustomizations.BasicTextFieldWithScrollbar
import com.alexvt.assistant.uitheme.Fonts
import com.alexvt.assistant.usecases.AiTranscribeFromMicStopRecordingUseCase
import com.alexvt.assistant.usecases.AiTranscribeFromMicUseCase
import com.alexvt.assistant.usecases.AiTransformTextUseCase
import com.alexvt.assistant.usecases.ExtractTextFromImageUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

@ExperimentalComposeUiApi
@FlowPreview
@Composable
fun AssistantView(globalBounds: Rect) {
    val mainThreadCoroutineScope = rememberCoroutineScope()
    val backgroundCoroutineScope = rememberCoroutineScope() + Dispatchers.Default
    val credentialsRepository = CredentialsRepository()
    val soundRecordingRepository = SoundRecordingRepository()
    val viewModel = remember {
        AssistantViewModel(
            mainThreadCoroutineScope,
            backgroundCoroutineScope,
            AiTransformTextUseCase(
                listOf(
                    AiTextCompleteCurieRepository(credentialsRepository),
                    AiTextChatRepository(credentialsRepository),
                    AiTextCompleteDaVinciRepository(credentialsRepository),
                )
            ),
            ExtractTextFromImageUseCase(ExtractableImageTextRepository()),
            AiTranscribeFromMicUseCase(
                soundRecordingRepository,
                AiSpeechTranscriptionRepository(credentialsRepository)
            ),
            AiTranscribeFromMicStopRecordingUseCase(soundRecordingRepository)
        )
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    val textFieldValue = remember { mutableStateOf(TextFieldValue(text = "")) }

    LaunchedEffect(Unit) {
        launch {
            viewModel.getUiEventFlow().collect { event ->
                when (event) {
                    is AssistantViewModel.ViewExternally -> {
                        openLinkInChrome(event.url)
                    }
                    is AssistantViewModel.TextGenerated -> {
                        textFieldValue.value =
                            TextFieldValue(
                                text = event.text,
                                selection = TextRange(event.text.length)
                            )
                        focusRequester.requestFocus()
                    }
                }
            }
        }
    }

    val uiState by viewModel.getUiStateFlow().collectAsState()

    if (uiState.isActive) {
        Box(
            Modifier
                .onKeyEvent {
                    if (it.key == Key.Escape && it.type == KeyEventType.KeyDown) {
                        viewModel.onEscape()
                    } else if (it.key == Key.F1 && it.type == KeyEventType.KeyDown) {
                        viewModel.onButtonClick(buttonIndex = 0)
                    } else if (it.key == Key.F2 && it.type == KeyEventType.KeyDown) {
                        viewModel.onButtonClick(buttonIndex = 1)
                    } else if (it.key == Key.F3 && it.type == KeyEventType.KeyDown) {
                        viewModel.onButtonClick(buttonIndex = 2)
                    } else if (it.key == Key.F4 && it.type == KeyEventType.KeyDown) {
                        viewModel.onButtonClick(buttonIndex = 3)
                    } else if (it.key == Key.F5 && it.type == KeyEventType.KeyDown) {
                        viewModel.onButtonClick(buttonIndex = 4)
                    } else if (it.key == Key.F6 && it.type == KeyEventType.KeyDown) {
                        viewModel.onButtonClick(buttonIndex = 5)
                    } else if (it.key == Key.F7 && it.type == KeyEventType.KeyDown) {
                        viewModel.onButtonClick(buttonIndex = 6)
                    } else if (it.key == Key.Enter && it.isCtrlPressed && it.type == KeyEventType.KeyDown) {
                        viewModel.onInputComplete()
                    } else if (it.key == Key.PrintScreen && it.isCtrlPressed && it.type == KeyEventType.KeyDown) {
                        viewModel.previewTakeScreenshot()
                    }
                    true
                }
                .pointerInput(uiState.isPickingScreenshot) {
                    awaitPointerEventScope {
                        while (uiState.isActive) { // todo dispose obsolete
                            val event = awaitPointerEvent()
                            val x = event.changes.first().position.x.toInt()
                            val y = event.changes.first().position.y.toInt()
                            if (event.changes.first().changedToUp()) {
                                viewModel.onFreeAreaPointerRelease(x, y, globalBounds.topLeft)
                            } else if (event.changes.first().changedToDown()) {
                                viewModel.onFreeAreaPointerDown(x, y)
                            } else if (event.changes.first().pressed) {
                                viewModel.onFreeAreaPointerMove(x, y)
                            }
                        }
                    }
                }
                .border(BorderStroke(1.dp, SolidColor(Color(0xFF00FF88))))
        ) {
            if (!uiState.isPreviewingScreenshot && !uiState.isPickingScreenshot) {
                Box(Modifier.width(800.dp)) { // todo wrap action buttons better
                    Column {
                        Surface {
                            // standard BasicTextField instead would have no scrollbar
                            BasicTextFieldWithScrollbar(
                                value = textFieldValue.value,
                                onValueChange = { newTextFieldValue ->
                                    textFieldValue.value = newTextFieldValue
                                    viewModel.onTextChanged(newTextFieldValue.text)
                                },
                                cursorBrush = SolidColor(Color(0xFFFFFFFF)),
                                textStyle = LocalTextStyle.current.copy(
                                    color = LocalContentColor.current.copy(alpha = 0.75f),
                                    lineHeight = 16.sp,
                                    fontSize = 13.sp,
                                    fontFamily = Fonts.jetbrainsMono(),
                                ),
                                modifier = Modifier
                                    .focusRequester(focusRequester)
                                    .heightIn(10.dp, 540.dp)
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            )
                        }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(Color(0xE0303030))
                                .height(36.dp)
                                .padding(horizontal = 6.dp)
                                .align(Alignment.CenterHorizontally)
                        ) {
                            Spacer(modifier = Modifier.weight(1f))
                            ActionButtons(
                                uiState.actionButtonNames,
                                uiState.actionIconTints,
                                selectedIndex = uiState.actionButtonSelectedIndex,
                            ) { index ->
                                viewModel.onButtonClick(index)
                            }
                            Spacer(modifier = Modifier.weight(1f))
                        }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(Color(0xE0303030))
                                .height(38.dp)
                                .padding(horizontal = 6.dp)
                                .align(Alignment.CenterHorizontally)
                        ) {
                            /*
                            Icon(
                                Icons.Default.Settings,
                                tint = Color(0xFFFFFFFF).copy(alpha = 0.6f),
                                contentDescription = "Settings",
                                modifier = Modifier.padding(6.dp).size(20.dp)
                            )
                            Icon(
                                Icons.Default.History,
                                tint = Color(0xFFFFFFFF).copy(alpha = 0.6f),
                                contentDescription = "History",
                                modifier = Modifier.padding(6.dp).size(20.dp)
                            )
                             */
                            Icon(
                                Icons.Default.DeleteSweep,
                                tint = Color(0xFFFFFFFF).copy(alpha = 0.6f),
                                contentDescription = "Clear",
                                modifier = Modifier.padding(6.dp).size(20.dp)
                                    .clickable {
                                        viewModel.clear()
                                    }
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = uiState.estimateText,
                                color = MaterialTheme.colors.primary.copy(alpha = 0.5f),
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                            if (uiState.actualCostText.isNotBlank()) {
                                Text(
                                    text = uiState.actualCostText,
                                    color = Color(0xFF668866),
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                            Icon(
                                Icons.Default.Mic,
                                tint = if (uiState.isRecordingFromMic) {
                                    Color(0xFFFF7777)
                                } else {
                                    Color(0xFFFFFFFF).copy(alpha = 0.6f)
                                },
                                contentDescription = "From voice recording",
                                modifier = Modifier.padding(6.dp).size(20.dp)
                                    .clickable {
                                        viewModel.onMicButton()
                                    }
                            )
                            Icon(
                                Icons.Default.Screenshot,
                                tint = Color(0xFFFFFFFF).copy(alpha = 0.6f),
                                contentDescription = "From screen",
                                modifier = Modifier.padding(6.dp).size(20.dp)
                                    .clickable {
                                        viewModel.previewTakeScreenshot()
                                    }
                            )
                        }
                    }
                    if (uiState.isBusyGettingResponse) {
                        Box(
                            Modifier
                                .background(Color(0x77303030))
                                .clickable { /* intercepted click */ }
                                .matchParentSize()
                        ) {
                            Text(
                                text = "Getting result...",
                                color = MaterialTheme.colors.primary,
                                modifier = Modifier.padding(10.dp)
                                    .width(400.dp)
                                    .height(IntrinsicSize.Min)
                                    .align(Alignment.BottomCenter)
                            )
                        }
                    }
                    if (uiState.isBusyGettingTextFromScreenshot) {
                        Box(
                            Modifier
                                .background(Color(0x77303030))
                                .clickable { /* intercept */ }
                                .matchParentSize()
                        ) {
                            Text(
                                text = "Getting text from screen area...",
                                color = MaterialTheme.colors.primary,
                                modifier = Modifier.padding(10.dp)
                                    .width(400.dp)
                                    .height(IntrinsicSize.Min)
                                    .align(Alignment.BottomCenter)
                            )
                        }
                    }
                    if (uiState.isBusyGettingTextFromMicRecording) {
                        Box(
                            Modifier
                                .background(Color(0x77303030))
                                .clickable { /* intercept */ }
                                .matchParentSize()
                        ) {
                            Text(
                                text = "Getting text from mic recording...",
                                color = MaterialTheme.colors.primary,
                                modifier = Modifier.padding(10.dp)
                                    .width(400.dp)
                                    .height(IntrinsicSize.Min)
                                    .align(Alignment.BottomCenter)
                            )
                        }
                    }
                }
            }
            if (uiState.isPreviewingScreenshot || uiState.isPickingScreenshot) {
                Box(
                    Modifier
                        .absoluteOffset(0.dp, 0.dp)
                        .size(globalBounds.width.dp, globalBounds.height.dp)
                ) {
                    if (uiState.isPickingScreenshot) {
                        Box(
                            Modifier
                                .offset(uiState.screenshotRectLeft.dp, uiState.screenshotRectTop.dp)
                                .width((uiState.screenshotRectRight - uiState.screenshotRectLeft).dp)
                                .height((uiState.screenshotRectBottom - uiState.screenshotRectTop).dp)
                                // standard non-dashed border instead would be
                                // .border(BorderStroke(2.dp, SolidColor(Color(0xFF303030))))
                                .dashedBorder(2.dp, 10f, Color(0xFF00FF88), Color(0xFF303030))
                        )
                    }
                }
            }
        }
    }
}

/**
 * Draws dashed border with the optional 2nd color between dashes
 */
private fun Modifier.dashedBorder(
    width: Dp,
    dashPitch: Float,
    color1: Color,
    color2: Color = Color.Transparent
) =
    drawBehind {
        drawIntoCanvas {
            it.drawRoundRect(
                width.toPx(),
                width.toPx(),
                size.width - width.toPx(),
                size.height - width.toPx(),
                0f, 0f,
                Paint()
                    .apply {
                        strokeWidth = width.toPx()
                        color = color2
                        style = Stroke
                    }
            )
            it.drawRoundRect(
                width.toPx(),
                width.toPx(),
                size.width - width.toPx(),
                size.height - width.toPx(),
                0f, 0f,
                Paint()
                    .apply {
                        strokeWidth = width.toPx()
                        color = color1
                        style = Stroke
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(dashPitch, dashPitch), 0f
                        )
                    }
            )
        }
    }
