package com.alexvt.assistant.repository

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class AiSpeechTranscriptionRepository(private val credentialsRepository: CredentialsRepository) {

    suspend fun getTranscription(
        audioBytes: ByteArray,
    ): Response {
        val apiResponse = HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 60_000
            }
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                })
            }
        }.apply {
            plugin(HttpSend).intercept { request ->
                println(request.body.toString())
                val response = execute(request) // todo log
                response
            }
        }.post("https://api.openai.com/v1/audio/transcriptions") {
            headers {
                append("Content-Type", "multipart/form-data")
                append("Authorization", "Bearer ${credentialsRepository.getOpenAiBearerToken()}")
            }
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("model", "whisper-1")
                        append("file", audioBytes, Headers.build {
                            append(HttpHeaders.ContentType, "audio/wav")
                            append(HttpHeaders.ContentDisposition, "filename=\"audio.wav\"")
                        })
                    }
                )
            )
        }.body<OpenAiTranscriptionResponseV1>()
        return Response(
            text = apiResponse.text, // todo compute costs estimate
        )
    }

    data class Response(
        val text: String,
    )

    @Serializable
    private data class OpenAiTranscriptionResponseV1(
        val text: String,
    )

}
