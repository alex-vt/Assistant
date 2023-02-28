package com.alexvt.assistant.repository

import com.alexvt.assistant.platform.extractFromScreen

class ExtractableImageTextRepository {

    fun extractFromScreenArea(top: Int, bottom: Int, left: Int, right: Int): String {
        return extractFromScreen(top, bottom, left, right)
    }

}
