package com.alexvt.assistant.repository

class AiTextCompleteCurieRepository(credentialsRepository: CredentialsRepository) :
    AiTextCompleteRepository(credentialsRepository) {

    override val model = LanguageModel(
        name = "text-curie-001",
        label = "Curie",
        maxTotalTokens = 2048,
        maxResponseTokens = 256,
        usdPerToken = 0.000002,
        timeoutMillis = 20_000,
    )
}
