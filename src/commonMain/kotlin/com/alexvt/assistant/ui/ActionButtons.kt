package com.alexvt.assistant.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

data class ActionButton(
    val text: String,
    val icon: ImageVector,
    val iconTint: Int,
    val isAlwaysShown: Boolean,
)

@Composable
fun ActionButtons(
    actionButtons: List<ActionButton>,
    selectedIndex: Int,
    onClick: (Int) -> Unit,
) {
    var availableWidth by remember { mutableStateOf(0) }
    Box(
        Modifier.onGloballyPositioned { layoutCoordinates ->
            availableWidth = layoutCoordinates.parentCoordinates?.size?.width ?: 0
        }
    ) {
        // fitting preferably all buttons and labels, gracefully degrading to less (if no space)
        val alwaysShownButtons = actionButtons.filter { actionButton ->
            actionButton.isAlwaysShown
        }
        PlaceFirstThatFits(
            fittingCondition = { placeable: Placeable ->
                placeable.width <= availableWidth
            },
            {
                ActionButtons(
                    actionButtons,
                    selectedIndex,
                    isLabeledButtons = true,
                    onClick
                )
            },
            {
                ActionButtons(
                    alwaysShownButtons,
                    selectedIndex,
                    isLabeledButtons = true,
                    onClick
                )
            },
            {
                ActionButtons(
                    actionButtons,
                    selectedIndex,
                    isLabeledButtons = false,
                    onClick
                )
            },
            {
                ActionButtons(
                    alwaysShownButtons,
                    selectedIndex,
                    isLabeledButtons = false,
                    onClick
                )
            },
        )
    }
}

/**
 * Passed views are measured. The first that fits, is placed.
 * If no views fit, the last one is placed.
 */
@Composable
private fun PlaceFirstThatFits(
    fittingCondition: (Placeable) -> Boolean,
    vararg candidateViews: @Composable () -> Unit,
) {
    SubcomposeLayout { constraints ->
        with(candidateViews) {
            dropWhile { candidateView ->
                val placeable = subcompose("$candidateView", candidateView)[0]
                    .measure(Constraints()) // unconstrained
                !fittingCondition(placeable)
            }.firstOrNull() ?: lastOrNull()
        }?.let { lastView ->
            val contentPlaceable = subcompose("placing_$lastView") { lastView() }[0]
                .measure(constraints)
            contentPlaceable.run {
                layout(width, height) {
                    place(0, 0)
                }
            }
        } ?: layout(0, 0) {}
    }
}

@Composable
private fun ActionButtons(
    actionButtons: List<ActionButton>,
    selectedIndex: Int,
    isLabeledButtons: Boolean,
    onClick: (Int) -> Unit,
) {
    Row {
        val cornerRadius = 14.dp

        actionButtons.forEachIndexed { index, item ->
            OutlinedButton(
                modifier = Modifier
                    .offset((-1 * index).dp, 0.dp)
                    .padding(start = if (index == 0) (actionButtons.size - 1).dp else 0.dp)
                    .widthIn(min = 50.dp)
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
                Icon(
                    item.icon,
                    tint = if (selectedIndex == index) {
                        Color(item.iconTint).copy(alpha = 0.8f)
                    } else {
                        Color(item.iconTint).copy(alpha = 0.5f)
                    },
                    contentDescription = item.text,
                    modifier = Modifier
                        .padding(
                            // outer buttons outer rounded edges need bigger padding
                            start = if (index == 0) 12.dp else 10.dp,
                            end = when {
                                isLabeledButtons -> 4.dp
                                index == actionButtons.size - 1 -> 12.dp
                                else -> 10.dp
                            },
                            top = 4.dp, bottom = 4.dp
                        )
                        .size(20.dp)
                )
                if (isLabeledButtons) {
                    Text(
                        text = item.text,
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
}