package com.alexvt.assistant.ui

import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Rect
import com.alexvt.assistant.uitheme.AppTheme
import kotlinx.coroutines.CoroutineDispatcher

@ExperimentalComposeUiApi
@Composable
fun MainView(
    globalBounds: Rect,
    backgroundDispatcher: CoroutineDispatcher,
) {

    DisableSelection {
        MaterialTheme(
            colors = AppTheme.colors.material
        ) {
            AssistantView(globalBounds, backgroundDispatcher)
        }
    }
}
