package com.alexvt.assistant.usecases

import com.alexvt.assistant.AppScope
import com.alexvt.assistant.repository.ExtractableImageTextRepository
import me.tatarka.inject.annotations.Inject

@AppScope
@Inject
class CheckTextFromScreenAvailabilityUseCase(
    private val extractableImageTextRepository: ExtractableImageTextRepository,
) {

    fun execute(): Boolean =
        extractableImageTextRepository.isExtractionAvailable()

}
