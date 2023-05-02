package com.alexvt.assistant.repository

import com.knuddels.jtokkit.Encodings
import com.knuddels.jtokkit.api.ModelType
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.util.network.UnresolvedAddressException
import java.util.concurrent.TimeoutException

abstract class AiTextRepository {

    protected data class LanguageModel(
        val label: String,
        val name: String,
        val maxTemperature: Double = 2.0,
        val maxTotalTokens: Int,
        val maxResponseTokens: Int, // a limit in the app, not of the language model
        val usdPerRequestToken: Double,
        val usdPerResponseToken: Double,
        val timeoutMillis: Long,
    )

    protected abstract val model: LanguageModel

    /**
     * A compute unit for OpenAI API is a token.
     * While response size in tokens is unknown and estimated at maximum,
     * request size in tokens is calculated and added to response size.
     */
    fun getComputeUnitsTotalEstimate(inputText: String): Int =
        getComputeUnitsRequestEstimate(inputText) + model.maxResponseTokens

    private fun getComputeUnitsRequestEstimate(requestText: String): Int {
        return Encodings.newDefaultEncodingRegistry()
            .getEncodingForModel(
                ModelType.valueOf(
                    model.name.uppercase().replace('-', '_').replace('.', '_')
                )
            )
            .countTokens(requestText)
    }

    fun getComputeUnitsTotalLimit(): Int =
        model.maxTotalTokens

    fun getComputeUnitsResponseLimit(): Int =
        model.maxResponseTokens

    fun getComputeUnitRequestCostUsd(): Double =
        model.usdPerRequestToken

    fun getComputeUnitResponseCostUsd(): Double =
        model.usdPerResponseToken

    fun getLanguageModel(): String =
        model.label

    data class Response(
        val text: String,
        val languageModel: String,
        val computeUnitsInRequest: Int,
        val computeUnitsInResponse: Int,
        val computeUnitRequestCostUsd: Double,
        val computeUnitResponseCostUsd: Double,
        val errorTitle: String,
        val errorText: String,
    )

    suspend fun getTransformed(inputText: String, normalizedRandomness: Double = 0.35): Response =
        try {
            getTransformedWithTemperature(
                inputText,
                temperature = model.maxTemperature * normalizedRandomness,
            )
        } catch (timeoutException: TimeoutException) {
            toErrorResponse(
                isBilledRequest = true,
                errorTitle = "Timeout of waiting for result from OpenAI",
                errorTextOrNull = with(model) {
                    "Max timeout for $label ($name) model: $timeoutMillis ms"
                },
            )
        } catch (unconvertedResponseException: UnconvertedResponseException) {
            toErrorResponse(
                isBilledRequest = isBilledRequest(unconvertedResponseException.message),
                errorTitle = "Failed to read text in result from OpenAI",
                errorTextOrNull = unconvertedResponseException.message,
            )
        } catch (unresolvedAddressException: UnresolvedAddressException) {
            toErrorResponse(
                isBilledRequest = false,
                errorTitle = "You are offline",
                errorTextOrNull = "Failed to establish connection to OpenAI to send text.",
            )
        } catch (throwable: Throwable) {
            toErrorResponse(
                isBilledRequest = false,
                errorTitle = "Unknown error",
                errorTextOrNull = with(throwable) { "$message \n${stackTraceToString()}".trim() },
            )
        }

    protected class UnconvertedResponseException(message: String, throwable: Throwable) :
        Exception(message, throwable)

    /**
     * API request assumed non-billable when the response contains "error" in JSON text.
     */
    private fun isBilledRequest(errorResponseText: String?): Boolean =
        !(errorResponseText ?: "").contains("\"error\"")

    private fun toErrorResponse(
        isBilledRequest: Boolean, // supposedly billed error response sizes unknown - considered max
        errorTitle: String,
        errorTextOrNull: String?,
    ): Response = with(model) {
        Response(
            text = "",
            languageModel = label,
            computeUnitsInRequest = if (isBilledRequest) {
                maxTotalTokens - maxResponseTokens
            } else {
                0
            },
            computeUnitsInResponse = if (isBilledRequest) maxResponseTokens else 0,
            computeUnitRequestCostUsd = usdPerRequestToken,
            computeUnitResponseCostUsd = usdPerResponseToken,
            errorTitle = errorTitle,
            errorText = errorTextOrNull ?: "",
        )
    }

    protected suspend fun HttpResponse.responseAsText(): String =
        "HTTP ${status.value}\n${bodyAsText().trim()}\n" +
                "in ${responseTime.timestamp - requestTime.timestamp} ms"

    protected abstract suspend fun getTransformedWithTemperature(
        inputText: String,
        temperature: Double
    ): Response

}
