package com.alexvt.assistant.repository

import java.io.File
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

actual class SoundRecordingRepository {

    private var lineOrNull: TargetDataLine? = null

    actual fun isMicAvailable(): Boolean =
        getLineOrNull() != null

    actual suspend fun getRecordingFromMic(): ByteArray =
        suspendCoroutine { continuation ->
            recordBlocking() // until finishRecording()
            continuation.resume(getAndResetRecording())
        }

    private fun recordBlocking() {
        lineOrNull = getLineOrNull()

        lineOrNull?.open(getWhisperCompatibleAudioFormat())
        lineOrNull?.start()
        println("Started recording")

        AudioSystem.write(
            AudioInputStream(lineOrNull),
            AudioFileFormat.Type.WAVE,
            getCurrentRecordingFile(),
        )
        println("Finished recording")
    }

    private fun getWhisperCompatibleAudioFormat(): AudioFormat =
        AudioFormat(16000.0f, 16, 1, true, false)

    private fun getLineOrNull(): TargetDataLine? =
        AudioSystem.getLine(
            DataLine.Info(TargetDataLine::class.java, getWhisperCompatibleAudioFormat())
        ) as? TargetDataLine

    actual fun finishRecording() {
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
