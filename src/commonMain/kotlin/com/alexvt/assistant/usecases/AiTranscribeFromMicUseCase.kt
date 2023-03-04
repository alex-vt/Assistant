package com.alexvt.assistant.usecases

import com.alexvt.assistant.repository.AiSpeechTranscriptionRepository
import com.alexvt.assistant.repository.AiSpeechTranscriptionRepository.Response
import com.alexvt.assistant.repository.SoundRecordingRepository

class AiTranscribeFromMicUseCase(
    private val soundRecordingRepository: SoundRecordingRepository,
    private val aiSpeechTranscriptionRepository: AiSpeechTranscriptionRepository,
) {

    suspend fun execute(): Response {
        val speechAudio = soundRecordingRepository.getRecordingFromMic()
        return aiSpeechTranscriptionRepository.getTranscription(speechAudio)
    }

}
