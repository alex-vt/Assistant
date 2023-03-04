package com.alexvt.assistant.usecases

import com.alexvt.assistant.repository.SoundRecordingRepository

class AiTranscribeFromMicStopRecordingUseCase(
    private val soundRecordingRepository: SoundRecordingRepository
) {

    fun execute() {
        soundRecordingRepository.finishRecording()
    }

}
