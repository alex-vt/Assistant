package com.alexvt.assistant.ui

import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Rect
import com.alexvt.assistant.uitheme.AppTheme

@ExperimentalComposeUiApi
@Composable
fun MainView(
    globalBounds: Rect
) {

    DisableSelection {
        MaterialTheme(
            colors = AppTheme.colors.material
        ) {
            AssistantView(globalBounds)
        }
    }
}
