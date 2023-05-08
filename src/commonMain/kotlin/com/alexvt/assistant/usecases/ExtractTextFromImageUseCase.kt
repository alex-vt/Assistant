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

    suspend fun execute(
        top: Int,
        bottom: Int,
        left: Int,
        right: Int,
        onImageCaptured: () -> Unit,
    ): String {
        val isImageBlank = abs(top - bottom) <= 1 || abs(left - right) <= 1
        if (isImageBlank) return ""
        return repository.extractFromScreenArea(top, bottom, left, right, onImageCaptured)
    }

}
