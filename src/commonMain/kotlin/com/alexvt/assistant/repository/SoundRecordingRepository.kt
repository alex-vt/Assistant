package com.alexvt.assistant.repository

import java.io.File
import javax.sound.sampled.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class SoundRecordingRepository {

    private var lineOrNull: TargetDataLine? = null

    suspend fun getRecordingFromMic(): ByteArray =
        suspendCoroutine { continuation ->
            recordBlocking() // until finishRecording()
            continuation.resume(getAndResetRecording())
        }

    private fun recordBlocking() {
        val whisperWavFormat = AudioFormat(16000.0f, 16, 1, true, false)

        lineOrNull = AudioSystem.getLine(
            DataLine.Info(TargetDataLine::class.java, whisperWavFormat)
        ) as TargetDataLine

        lineOrNull?.open(whisperWavFormat)
        lineOrNull?.start()
        println("Started recording")

        AudioSystem.write(
            AudioInputStream(lineOrNull),
            AudioFileFormat.Type.WAVE,
            getCurrentRecordingFile(),
        )
        println("Finished recording")
    }

    fun finishRecording() {
        lineOrNull?.stop()
        lineOrNull?.close()
    }

    private fun getAndResetRecording(): ByteArray {
        return getCurrentRecordingFile().readBytes().also {
            getCurrentRecordingFile().delete()
            println("Recorded ${it.size} bytes")
        }
    }

    private fun getCurrentRecordingFile(): File =
        File("AssistantCurrentRecording.wav")

}
