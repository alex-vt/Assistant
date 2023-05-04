package com.alexvt.assistant

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
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
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter,
            ) {
                MainView(dependencies, getGlobalBounds(), Dispatchers.Default)
            }
        }
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
}
