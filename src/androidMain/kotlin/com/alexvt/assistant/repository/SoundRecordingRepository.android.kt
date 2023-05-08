package com.alexvt.assistant.repository

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioRecord.RECORDSTATE_RECORDING
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.alexvt.assistant.App.Companion.androidAppContext
import com.alexvt.assistant.repository.SoundRecordingRepository.MicrophonePermissionHandlingActivity.Companion.permissionOutcomeOrNull
import kotlinx.coroutines.delay
import java.nio.ByteBuffer
import java.nio.ByteOrder

actual class SoundRecordingRepository {

    private var recorderOrNull: AudioRecord? = null

    actual fun isMicAvailable(): Boolean =
        androidAppContext.packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)

    actual suspend fun getRecordingFromMic(): ByteArray {
        // OpenAI Whisper default audio input format
        val whisperFormatSampleRate = 16000
        val whisperFormatChannels = 1
        val whisperFormatEncoding = AudioFormat.ENCODING_PCM_16BIT
        val whisperFormatBitDepth = 16
        val bufferSize = AudioRecord.getMinBufferSize(
            whisperFormatSampleRate, whisperFormatChannels, whisperFormatEncoding
        )

        // recording starts with permission, otherwise returns as empty right away
        if (checkOrGetMicPermission()) {
            recorderOrNull = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                whisperFormatSampleRate, whisperFormatChannels,
                whisperFormatEncoding, bufferSize
            ).apply {
                startRecording()
            }
        } else {
            Log.e("AssistantLog", "Recording permission denied")
            return byteArrayOf()
        }

        val buffer = ByteArray(bufferSize)
        var bytesRead: Int
        var recordingBytes = byteArrayOf()
        while (recorderOrNull?.recordingState == RECORDSTATE_RECORDING) { // todo limit size
            recorderOrNull?.run {
                bytesRead = read(buffer, 0, buffer.size)
                recordingBytes += buffer.take(bytesRead)
            }
        }
        recorderOrNull = null

        if (recordingBytes.isEmpty()) {
            Log.e("AssistantLog", "Failed to obtain recording")
        }
        return getWavHeader(
            channels = whisperFormatChannels.toShort(),
            sampleRate = whisperFormatSampleRate,
            bitDepth = whisperFormatBitDepth.toShort()
        ) + recordingBytes
    }

    actual fun finishRecording() {
        recorderOrNull?.run {
            stop()
            release()
        }
    }

    // see https://gist.github.com/kmark/d8b1b01fb0d2febf5770
    private fun getWavHeader(
        channels: Short,
        sampleRate: Int,
        bitDepth: Short
    ): ByteArray {
        // Convert the multi-byte integers to raw bytes in little endian format as required by the spec
        val littleBytes = ByteBuffer
            .allocate(14)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putShort(channels)
            .putInt(sampleRate)
            .putInt(sampleRate * channels * (bitDepth / 8))
            .putShort((channels * (bitDepth / 8)).toShort())
            .putShort(bitDepth)
            .array()

        return byteArrayOf( // RIFF header
            'R'.code.toByte(),
            'I'.code.toByte(),
            'F'.code.toByte(),
            'F'.code.toByte(),  // ChunkID
            0,
            0,
            0,
            0,  // ChunkSize (must be updated later)
            'W'.code.toByte(),
            'A'.code.toByte(),
            'V'.code.toByte(),
            'E'.code.toByte(),  // Format
            // fmt subchunk
            'f'.code.toByte(),
            'm'.code.toByte(),
            't'.code.toByte(),
            ' '.code.toByte(),  // Subchunk1ID
            16,
            0,
            0,
            0,  // Subchunk1Size
            1,
            0,  // AudioFormat
            littleBytes[0],
            littleBytes[1],  // NumChannels
            littleBytes[2],
            littleBytes[3],
            littleBytes[4],
            littleBytes[5],  // SampleRate
            littleBytes[6],
            littleBytes[7],
            littleBytes[8],
            littleBytes[9],  // ByteRate
            littleBytes[10],
            littleBytes[11],  // BlockAlign
            littleBytes[12],
            littleBytes[13],  // BitsPerSample
            // data subchunk
            'd'.code.toByte(),
            'a'.code.toByte(),
            't'.code.toByte(),
            'a'.code.toByte(),  // Subchunk2ID
            0,
            0,
            0,
            0
        )
    }

    private suspend fun checkOrGetMicPermission(): Boolean {
        // an already granted permission won't trigger re-requesting
        if (
            ContextCompat.checkSelfPermission(
                androidAppContext, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        // at this point permission is not granted, an overlay activity requests it
        androidAppContext.startActivity(
            Intent(androidAppContext, MicrophonePermissionHandlingActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
        while (true) {
            delay(100) // not granted or denied yet...
            permissionOutcomeOrNull?.let { return it }
        }
    }

    class MicrophonePermissionHandlingActivity : Activity() {

        companion object {
            var permissionOutcomeOrNull: Boolean? = null
            private const val REQUEST_CODE = 1
        }


        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            permissionOutcomeOrNull = null
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_CODE
            )
        }

        override fun onRequestPermissionsResult(
            requestCode: Int, permissions: Array<out String>, grantResults: IntArray
        ) {
            if (requestCode == REQUEST_CODE && grantResults.isNotEmpty()
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                permissionOutcomeOrNull = true
            } else {
                permissionOutcomeOrNull = false
                Toast.makeText(this, "Recording permission denied", Toast.LENGTH_SHORT).show()
            }
            finish()
        }
    }

}
