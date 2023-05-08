package com.alexvt.assistant.repository

import com.alexvt.assistant.AppScope
import me.tatarka.inject.annotations.Inject

@AppScope
@Inject
expect class ExtractableImageTextRepository() {

    fun isExtractionAvailable(): Boolean

    suspend fun extractFromScreenArea(
        top: Int,
        bottom: Int,
        left: Int,
        right: Int,
        onImageCaptured: () -> Unit,
    ): String

}
