package com.alexvt.assistant

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Rect
import com.alexvt.assistant.App.Companion.dependencies
import com.alexvt.assistant.platform.androidContext
import com.alexvt.assistant.ui.MainView
import kotlinx.coroutines.Dispatchers

@ExperimentalComposeUiApi
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidContext = this

        val globalBounds = Rect(0f, 0f, 0f, 0f)
        setContent {
            MainView(dependencies, globalBounds, Dispatchers.Default)
        }

    }
}
