package com.alexvt.assistant.uicustomizations

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import com.alexvt.assistant.platform.VerticalScrollbar
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BasicTextFieldWithScrollbar(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = TextStyle.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    cursorBrush: Brush = SolidColor(Color.Black),
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit =
        @Composable { innerTextField -> innerTextField() }
) {
    Box {
        val coroutineScope = rememberCoroutineScope()
        val textLayoutResultOrNull = remember { mutableStateOf<TextLayoutResult?>(null) }
        val scrollState = remember { mutableStateOf(ScrollState(initial = 0)) }
        val bringIntoViewRequester = remember { BringIntoViewRequester() }
        BasicTextField(
            value = value,
            onValueChange = { newValue ->
                if (newValue.text == value.text) {
                    // only selection changed. Bring cursor into view using old text layout
                    coroutineScope.launch {
                        textLayoutResultOrNull.value?.run {
                            bringIntoViewRequester.bringIntoView(getCursorRect(value.selection.end))
                        }
                    }
                } else {
                    // text changed. Wait for onTextLayout and bring cursor into view there
                }
                onValueChange(newValue)
            },
            modifier = modifier.verticalScroll(scrollState.value)
                .bringIntoViewRequester(bringIntoViewRequester),
            enabled = enabled,
            readOnly = readOnly,
            textStyle = textStyle,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            maxLines = maxLines,
            visualTransformation = visualTransformation,
            onTextLayout = { textLayoutResult ->
                textLayoutResultOrNull.value = textLayoutResult
                coroutineScope.launch {
                    textLayoutResultOrNull.value?.run {
                        try {
                            bringIntoViewRequester.bringIntoView(getCursorRect(value.selection.end))
                        } catch (t: Throwable) {
                            // because of async recompositions,
                            // needed just for desktop window to resize to wrap content
                            println("bringIntoView exception")
                        }
                    }
                }
                onTextLayout(textLayoutResult)
            },
            interactionSource = interactionSource,
            cursorBrush = cursorBrush,
            decorationBox = decorationBox
        )
        Row(Modifier.matchParentSize()) {
            Box(Modifier.weight(1f))
            VerticalScrollbar(
                Modifier, scrollState.value
            )
        }
    }
}
