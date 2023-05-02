package com.alexvt.assistant.usecases

import com.alexvt.assistant.AppScope
import com.alexvt.assistant.repository.ExtractableImageTextRepository
import me.tatarka.inject.annotations.Inject
import kotlin.math.abs

@AppScope
@Inject
class ExtractTextFromImageUseCase(
    private val repository: ExtractableImageTextRepository,
) {

    fun execute(top: Int, bottom: Int, left: Int, right: Int): String {
        val isImageBlank = abs(top - bottom) <= 1 || abs(left - right) <= 1
        if (isImageBlank) return ""
        return repository.extractFromScreenArea(top, bottom, left, right)
    }

}
