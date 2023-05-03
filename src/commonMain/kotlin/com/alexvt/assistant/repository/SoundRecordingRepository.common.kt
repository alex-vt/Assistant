package com.alexvt.assistant.repository

import com.alexvt.assistant.AppScope
import me.tatarka.inject.annotations.Inject

@AppScope
@Inject
expect class SoundRecordingRepository() {

    fun isMicAvailable(): Boolean

    suspend fun getRecordingFromMic(): ByteArray

    fun finishRecording()

}
