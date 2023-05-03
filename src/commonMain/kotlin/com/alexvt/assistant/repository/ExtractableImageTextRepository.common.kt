package com.alexvt.assistant.repository

import com.alexvt.assistant.AppScope
import me.tatarka.inject.annotations.Inject

@AppScope
@Inject
expect class ExtractableImageTextRepository {

    fun isExtractionAvailable(): Boolean

    fun extractFromScreenArea(top: Int, bottom: Int, left: Int, right: Int): String

}
