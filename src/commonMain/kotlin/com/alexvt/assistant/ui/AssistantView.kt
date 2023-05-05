package com.alexvt.assistant.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.RestorePage
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alexvt.assistant.AppDependencies
import com.alexvt.assistant.platform.openLinkInChrome
import com.alexvt.assistant.uicustomizations.BasicTextFieldWithBestEffortScrollbar
import com.alexvt.assistant.uitheme.Fonts
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch
import moe.tlaster.precompose.ui.viewModel

@ExperimentalComposeUiApi
@FlowPreview
@Composable
fun AssistantView(
    dependencies: AppDependencies,
    globalBounds: Rect,
    backgroundDispatcher: CoroutineDispatcher,
) {
    val viewModel = viewModel(AssistantViewModel::class) {
        AssistantViewModel(dependencies.assistantViewModelUseCases, backgroundDispatcher)
    }
    val uiState by viewModel.getUiStateFlow().collectAsState()

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(text = uiState.text, selection = TextRange(uiState.text.length))
        )
    }
    val density = LocalDensity.current.density

    LaunchedEffect(Unit) {
        launch {
            viewModel.getUiEventFlow().collect { event ->
                when (event) {
                    is AssistantViewModel.ViewExternally -> {
                        openLinkInChrome(event.url)
                    }

                    is AssistantViewModel.TextGenerated -> {
                        textFieldValue =
                            TextFieldValue(
                                text = event.text,
                                selection = TextRange(event.text.length),
                            )
                        focusRequester.requestFocus()
                    }
                }
            }
        }
    }

    if (uiState.isActive) {
        Box(
            Modifier
                .onKeyEvent {
                    // modifier keys only
                    if (it.key == Key.CtrlRight || it.key == Key.ShiftRight) {
                        if (it.isShiftPressed && it.isCtrlPressed) {
                            viewModel.onInstructionModelSelectMax()
                        } else if (it.isShiftPressed) {
                            viewModel.onInstructionModelSelectMedium()
                        } else if (it.isCtrlPressed) {
                            viewModel.onInstructionModelSelectMin()
                        } else {
                            viewModel.onInstructionModelUnselect()
                        }
                    }
                    // other keys
                    if (it.type == KeyEventType.KeyDown) {
                        val isAction = it.isShiftPressed || it.isCtrlPressed
                        when (it.key) {
                            Key.Escape -> viewModel.onEscape()
                            Key.F1 -> viewModel.onActionButton(buttonIndex = 0, isClick = isAction)
                            Key.F2 -> viewModel.onActionButton(buttonIndex = 1, isClick = isAction)
                            Key.F3 -> viewModel.onActionButton(buttonIndex = 2, isClick = isAction)
                            Key.F4 -> viewModel.onActionButton(buttonIndex = 3, isClick = isAction)
                            Key.F5 -> viewModel.onActionButton(buttonIndex = 4, isClick = isAction)
                            Key.F6 -> viewModel.onActionButton(buttonIndex = 5, isClick = isAction)
                            Key.F7 -> viewModel.onActionButton(buttonIndex = 6, isClick = isAction)
                            Key.Enter -> {
                                if (isAction) {
                                    viewModel.onInputEnter()
                                }
                            }

                            Key.PrintScreen -> {
                                if (it.isCtrlPressed) {
                                    viewModel.previewTakeScreenshot()
                                }
                            }
                        }
                    }
                    true
                }
                .pointerInput(uiState.isPickingScreenshot) {
                    awaitPointerEventScope {
                        while (uiState.isActive) { // todo dispose obsolete
                            val event = awaitPointerEvent()
                            val x = (event.changes.first().position.x / density).toInt()
                            val y = (event.changes.first().position.y / density).toInt()
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
                Box(Modifier.heightIn(max = 566.dp).widthIn(max = 798.dp)) {
                    Column(
                        Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            Modifier.weight(1f, fill = false).fillMaxWidth()
                        ) {
                            // standard BasicTextField instead would have no scrollbar
                            BasicTextFieldWithBestEffortScrollbar(
                                value = textFieldValue,
                                onValueChange = { newTextFieldValue ->
                                    viewModel.onTextChanged(newTextFieldValue.text)
                                    textFieldValue = newTextFieldValue
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
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            )
                        }
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .background(Color(0xE0303030))
                                .height(36.dp)
                                .padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 2.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            ActionButtons(
                                actionButtons = uiState.actionButtonNames.run {
                                    val actionIconMap = mapOf(
                                        "YouTube" to ActionButton(
                                            text = "YouTube",
                                            icon = Icons.Default.OndemandVideo,
                                            iconTint = uiState.actionIconTints[0],
                                            isAlwaysShown = true,
                                        ),
                                        "W|Alpha" to ActionButton(
                                            text = "W|Alpha",
                                            icon = Icons.Default.Science,
                                            iconTint = uiState.actionIconTints[1],
                                            isAlwaysShown = true,
                                        ),
                                        "Google" to ActionButton(
                                            text = "Google",
                                            icon = Icons.Default.Search,
                                            iconTint = uiState.actionIconTints[2],
                                            isAlwaysShown = true,
                                        ),
                                        "Answer" to ActionButton(
                                            text = "Answer",
                                            icon = Icons.Default.QuestionAnswer,
                                            iconTint = uiState.actionIconTints[3],
                                            isAlwaysShown = true,
                                        ),
                                        "Summary" to ActionButton(
                                            text = "Summary",
                                            icon = Icons.Default.List,
                                            iconTint = uiState.actionIconTints[4],
                                            isAlwaysShown = true,
                                        ),
                                        "Rewrite" to ActionButton(
                                            text = "Rewrite",
                                            icon = Icons.Default.RestorePage,
                                            iconTint = uiState.actionIconTints[5],
                                            isAlwaysShown = false,
                                        ),
                                        "Continue" to ActionButton(
                                            text = "Continue",
                                            icon = Icons.Default.PlaylistPlay,
                                            iconTint = uiState.actionIconTints[6],
                                            isAlwaysShown = true,
                                        ),
                                    )
                                    map { name ->
                                        actionIconMap.getValue(name)
                                    }
                                },
                                selectedIndex = uiState.actionButtonSelectedIndex,
                            ) { index ->
                                viewModel.onActionButton(index, isClick = true)
                            }
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
                                        viewModel.clearText()
                                    }
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            if (uiState.estimateText.isNotBlank()) {
                                Text(
                                    text = uiState.estimateText,
                                    color = MaterialTheme.colors.primary.copy(alpha = 0.5f),
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                            if (uiState.actualCostText.isNotBlank()) {
                                Text(
                                    text = uiState.actualCostText,
                                    color = Color(0xFF668866),
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                            if (uiState.isMicButtonVisible) {
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
                            }
                            if (uiState.isScreenshotButtonVisible) {
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
                            Icon(
                                Icons.Default.Send,
                                tint = Color(0xFF00FF88).copy(alpha = 0.6f),
                                contentDescription = "Run selected action",
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp)
                                    .size(20.dp)
                                    .clickable {
                                        viewModel.onInputEnter()
                                    }
                            )
                        }
                        if (uiState.isShowingError) {
                            Text(
                                text = uiState.errorTitle,
                                color = Color(0xFFFF9999),
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xE0303030))
                                    .padding(horizontal = 12.dp)
                            )
                            Text(
                                text = uiState.errorDetails,
                                color = Color(0xFFFF9999).copy(alpha = 0.8f),
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xE0303030))
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
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
