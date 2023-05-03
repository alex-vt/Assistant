package com.alexvt.assistant.repository

import com.alexvt.assistant.AppScope
import me.tatarka.inject.annotations.Inject

@AppScope
@Inject
actual class ExtractableImageTextRepository {

    actual fun isExtractionAvailable(): Boolean =
        false // todo implement

    actual fun extractFromScreenArea(top: Int, bottom: Int, left: Int, right: Int): String {
        throw NotImplementedError() // todo implement
    }

}
