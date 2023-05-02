package com.alexvt.assistant.repository

import com.alexvt.assistant.AppScope
import me.tatarka.inject.annotations.Inject

@AppScope
@Inject
class AiTextCompleteCurieRepository(credentialsRepository: CredentialsRepository) :
    AiTextCompleteRepository(credentialsRepository) {

    override val model = LanguageModel(
        name = "text-curie-001",
        label = "Curie",
        maxTotalTokens = 2048,
        maxResponseTokens = 256,
        usdPerRequestToken = 0.000002,
        usdPerResponseToken = 0.000002,
        timeoutMillis = 60_000,
    )
}
