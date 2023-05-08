package com.alexvt.assistant.usecases

import com.alexvt.assistant.AppScope
import com.alexvt.assistant.repository.AiSpeechTranscriptionRepository
import com.alexvt.assistant.repository.AiSpeechTranscriptionRepository.Response
import com.alexvt.assistant.repository.SoundRecordingRepository
import me.tatarka.inject.annotations.Inject

@AppScope
@Inject
class AiTranscribeFromMicUseCase(
    private val soundRecordingRepository: SoundRecordingRepository,
    private val aiSpeechTranscriptionRepository: AiSpeechTranscriptionRepository,
) {

    suspend fun execute(): Response {
        val speechAudio = soundRecordingRepository.getRecordingFromMic()
        if (speechAudio.isEmpty()) return Response(text = "")
        return aiSpeechTranscriptionRepository.getTranscription(speechAudio)
    }

}
