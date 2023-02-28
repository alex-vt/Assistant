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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberTrayState
import com.alexvt.assistant.ui.MainView
import com.tulskiy.keymaster.common.Provider
import kotlinx.coroutines.FlowPreview
import java.awt.Rectangle
import java.awt.Toolkit
import javax.swing.KeyStroke

@ExperimentalFoundationApi
@FlowPreview
@ExperimentalComposeUiApi
fun main() = application {
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
            Window(
                onCloseRequest = ::exitApplication,
                title = "Assistant",
                undecorated = true,
                transparent = true,
                resizable = false,
                //alwaysOnTop = true,
                icon = object : Painter() {
                    override val intrinsicSize = Size(256f, 256f)

                    override fun DrawScope.onDraw() {
                        drawOval(Color(0xAA00FF77), Offset(0f, 0f), Size(size.width, size.height))
                    }
                },
                //BitmapPainter(useResource("ic_launcher.png", ::loadImageBitmap)),
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
                    MainView(globalBounds.value)
                }
            }
        }

        val trayState = rememberTrayState()
        Tray(
            state = trayState,
            icon = object : Painter() { // todo alpha background
                override val intrinsicSize = Size(256f, 256f)

                override fun DrawScope.onDraw() {
                    drawOval(
                        Color(0x77FFFF77),
                        Offset(64f, 64f),
                        Size(size.width / 2, size.height / 2)
                    )
                }
            },
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