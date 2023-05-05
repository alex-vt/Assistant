package com.alexvt.assistant

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalFocusManager
import com.alexvt.assistant.App.Companion.dependencies
import com.alexvt.assistant.platform.androidContext
import com.alexvt.assistant.ui.MainView
import kotlinx.coroutines.Dispatchers
import moe.tlaster.precompose.lifecycle.PreComposeActivity
import moe.tlaster.precompose.lifecycle.setContent

@ExperimentalComposeUiApi
class MainActivity : PreComposeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidContext = this

        setContent {
            fixFocusStuckOnSoftKeyboardHide(
                findViewById(android.R.id.content), LocalFocusManager.current
            )
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter,
            ) {
                MainView(dependencies, getGlobalBounds(), Dispatchers.Default)
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
