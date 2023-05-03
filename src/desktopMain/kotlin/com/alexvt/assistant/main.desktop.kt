package com.alexvt.assistant

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberTrayState
import com.alexvt.assistant.ui.MainView
import com.tulskiy.keymaster.common.Provider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import moe.tlaster.precompose.PreComposeWindow
import java.awt.Rectangle
import java.awt.Toolkit
import javax.swing.KeyStroke

@ExperimentalFoundationApi
@FlowPreview
@ExperimentalComposeUiApi
fun main() = application {
    val dependencies: AppDependencies = remember { AppDependencies::class.create() }
    val isAppRunning = remember { mutableStateOf(true) }
    val isMainWindowShowing = remember { mutableStateOf(true) } // todo false on prod
    val globalHotkeyProvider = remember {
        Provider.getCurrentProvider(false).apply {
            register(KeyStroke.getKeyStroke("meta SPACE")) {
                isMainWindowShowing.value = !isMainWindowShowing.value
            }
        }
    }
    if (isAppRunning.value) {
        if (isMainWindowShowing.value) {
            PreComposeWindow(
                onCloseRequest = ::exitApplication,
                title = "Assistant",
                undecorated = true,
                transparent = true,
                resizable = false,
                //alwaysOnTop = true,
                icon = BitmapPainter(useResource("app_icon.png", ::loadImageBitmap)),
            ) {
                val globalBounds = remember { mutableStateOf(Rect(0f, 0f, 0f, 0f)) }
                Box(
                    Modifier
                        .wrapContentHeight(Alignment.Top, unbounded = true)
                        .wrapContentWidth(unbounded = true)
                        .onGloballyPositioned { layoutCoordinates ->
                            globalBounds.value = Rect(
                                left = 0f, // todo get left bound (taskbar can be on the left)
                                top = if (window.isVisible) {
                                    window.locationOnScreen.y.toFloat()
                                } else {
                                    0f
                                },
                                right = Toolkit.getDefaultToolkit().screenSize.width.toFloat(),
                                bottom = Toolkit.getDefaultToolkit().screenSize.height.toFloat(),
                            )
                            window.bounds = Rectangle(
                                (globalBounds.value.width.toInt() - layoutCoordinates.size.width) / 2,
                                globalBounds.value.top.toInt(),
                                layoutCoordinates.size.width,
                                layoutCoordinates.size.height
                            )
                            if (layoutCoordinates.size.height == 0) {
                                isMainWindowShowing.value = false
                                window.dispose()
                            }
                        }
                ) {
                    MainView(dependencies, globalBounds.value, Dispatchers.Default)
                }
            }
        }

        val trayState = rememberTrayState()
        Tray(
            state = trayState,
            icon = BitmapPainter(useResource("app_icon.png", ::loadImageBitmap)),
            menu = {
                Item(
                    "Show Assistant",
                    onClick = {
                        isMainWindowShowing.value = true
                    }
                )
                Item(
                    "Exit",
                    onClick = {
                        isAppRunning.value = false
                        globalHotkeyProvider.reset()
                        globalHotkeyProvider.stop()
                    }
                )
            }
        )

    }
}