package com.alexvt.assistant.repository

actual class SoundRecordingRepository {

    actual fun isMicAvailable(): Boolean =
        false // todo implement

    actual suspend fun getRecordingFromMic(): ByteArray =
        throw NotImplementedError() // todo implement

    actual fun finishRecording() {
        throw NotImplementedError() // todo implement
    }

}
