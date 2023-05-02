package com.alexvt.assistant.repository

import com.alexvt.assistant.AppScope
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.plugin
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.tatarka.inject.annotations.Inject

@AppScope
@Inject
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
