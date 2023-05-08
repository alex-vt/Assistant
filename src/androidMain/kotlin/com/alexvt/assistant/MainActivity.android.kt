package com.alexvt.assistant

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.alexvt.assistant.App.Companion.dependencies
import com.alexvt.assistant.ui.MainView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import moe.tlaster.precompose.lifecycle.PreComposeActivity
import moe.tlaster.precompose.lifecycle.setContent

@ExperimentalComposeUiApi
class MainActivity : PreComposeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            fixFocusStuckOnSoftKeyboardHide(
                findViewById(android.R.id.content), LocalFocusManager.current
            )
            Column(Modifier.fillMaxSize()) {
                Spacer(
                    // shade fill over non-clickable (Android specific) other apps UI
                    Modifier.fillMaxWidth().weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            finish()
                        }.background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0x00000000), Color(0x50000000)),
                            )
                        )
                )
                val coroutineScope = rememberCoroutineScope()
                var finishWithDelayJobOrNull by remember { mutableStateOf<Job?>(null) }
                Row(
                    Modifier.fillMaxWidth().onGloballyPositioned { layoutCoordinates ->
                        // Assistant UI can collapse itself "out of activity".
                        // UI reshape-related height instantaneous fluctuations don't count.
                        when (layoutCoordinates.size.height) {
                            0 -> {
                                finishWithDelayJobOrNull = coroutineScope.launch {
                                    delay(100)
                                    finish()
                                }
                            }

                            else -> {
                                finishWithDelayJobOrNull?.cancel()
                            }
                        }
                    }
                ) {
                    // Side spacers appear on wide screen
                    Spacer(
                        Modifier.weight(1f).height(100.dp).background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0x50000000), Color(0x00000000)),
                            )
                        )
                    )
                    MainView(dependencies, getGlobalBounds(), Dispatchers.Default)
                    Spacer(
                        Modifier.weight(1f).height(100.dp).background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0x50000000), Color(0x00000000)),
                            )
                        )
                    )
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish() // enforcing this button behavior on all Android versions
            }
        })
    }

    private fun getGlobalBounds(): Rect {
        val density = resources.displayMetrics.density
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        val left = 0
        val top = (getStatusBarHeight() / density).toInt()
        val right = (screenWidth / density).toInt()
        val bottom = (screenHeight / density).toInt() + top
        return Rect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
    }

    @SuppressLint("DiscouragedApi", "InternalInsetResource")
    fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    // see https://stackoverflow.com/questions/68389802
    // see https://issuetracker.google.com/issues/192433071
    private fun fixFocusStuckOnSoftKeyboardHide(contentView: View, focusManager: FocusManager) {
        var isSoftKeyboardOpen = true
        contentView.viewTreeObserver.addOnGlobalLayoutListener {
            val r = android.graphics.Rect()
            contentView.getWindowVisibleDisplayFrame(r)
            val screenHeight = contentView.rootView.height
            val keypadHeight = screenHeight - r.bottom
            if (keypadHeight > screenHeight * 0.15) { // ratio to determine keyboard height
                isSoftKeyboardOpen = true
                focusManager.moveFocus(FocusDirection.Next)
            } else if (isSoftKeyboardOpen) {
                isSoftKeyboardOpen = false
                focusManager.clearFocus()
            }
        }
    }
}
