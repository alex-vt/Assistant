package com.alexvt.assistant.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@Composable
fun ActionButtons(
    buttonNames: List<String>,
    iconTints: List<Int>,
    selectedIndex: Int,
    onClick: (Int) -> Unit
) {
    val actionIconMap = mapOf(
        "YouTube" to Icons.Default.OndemandVideo,
        "W|Alpha" to Icons.Default.Science,
        "Google" to Icons.Default.Search,
        "Answer" to Icons.Default.QuestionAnswer,
        "Summary" to Icons.Default.List,
        "Rewrite" to Icons.Default.RestorePage,
        "Continue" to Icons.Default.PlaylistPlay,
    )
    val defaultIcon = Icons.Default.List
    val actionButtons = buttonNames.map { name ->
        name to actionIconMap.getOrDefault(name, defaultIcon)
    }

    val cornerRadius = 14.dp

    actionButtons.forEachIndexed { index, item ->
        OutlinedButton(
            modifier = when (index) {
                0 ->
                    Modifier.offset(0.dp, 0.dp)
                else ->
                    Modifier.offset((-1 * index).dp, 0.dp)
            }
                .heightIn(min = 1.dp)
                .widthIn(min = 1.dp)
                .padding(top = 6.dp, bottom = 2.dp)
                .zIndex(if (selectedIndex == index) 1f else 0f),
            onClick = {
                onClick(index)
            },
            shape = when (index) {
                // left outer button
                0 -> RoundedCornerShape(
                    topStart = cornerRadius,
                    topEnd = 0.dp,
                    bottomStart = cornerRadius,
                    bottomEnd = 0.dp
                )
                // right outer button
                actionButtons.size - 1 -> RoundedCornerShape(
                    topStart = 0.dp,
                    topEnd = cornerRadius,
                    bottomStart = 0.dp,
                    bottomEnd = cornerRadius
                )
                // middle button
                else -> RoundedCornerShape(
                    topStart = 0.dp,
                    topEnd = 0.dp,
                    bottomStart = 0.dp,
                    bottomEnd = 0.dp
                )
            },
            border = BorderStroke(
                1.dp, if (selectedIndex == index) {
                    MaterialTheme.colors.primary.copy(alpha = 0.8f)
                } else {
                    MaterialTheme.colors.primary.copy(alpha = 0.25f)
                }
            ),
            colors = if (selectedIndex == index) {
                // selected colors
                ButtonDefaults.outlinedButtonColors(
                    backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.1f),
                    contentColor = MaterialTheme.colors.primary
                )
            } else {
                // not selected colors
                ButtonDefaults.outlinedButtonColors(
                    backgroundColor = MaterialTheme.colors.surface,
                    contentColor = MaterialTheme.colors.primary
                )
            },
            contentPadding = PaddingValues(0.dp),
        ) {
            val screenWidth = 800 // todo fit in given width
            Icon(
                item.second,
                tint = if (selectedIndex == index) {
                    Color(iconTints[index]).copy(alpha = 0.8f)
                } else {
                    Color(iconTints[index]).copy(alpha = 0.5f)
                },
                contentDescription = item.first,
                modifier = Modifier
                    .padding(
                        start = 10.dp,
                        end = if (screenWidth > 500) 4.dp else 10.dp,
                        top = 4.dp, bottom = 4.dp
                    )
                    .size(20.dp)
            )
            if (screenWidth > 500) {
                Text(
                    text = item.first,
                    color = if (selectedIndex == index) {
                        MaterialTheme.colors.primary
                    } else {
                        MaterialTheme.colors.primary.copy(alpha = 0.5f)
                    },
                    modifier = Modifier.padding(
                        end = 12.dp,
                        top = 5.dp,
                        bottom = 5.dp
                    )
                )
            }
        }
    }
}
