package com.alexvt.assistant.usecases

import com.alexvt.assistant.AppScope
import com.alexvt.assistant.repository.SoundRecordingRepository
import me.tatarka.inject.annotations.Inject

@AppScope
@Inject
class CheckMicAvailabilityUseCase(
    private val soundRecordingRepository: SoundRecordingRepository,
) {

    fun execute(): Boolean =
        soundRecordingRepository.isMicAvailable()

}
