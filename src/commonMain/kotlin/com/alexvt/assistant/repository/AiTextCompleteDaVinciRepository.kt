package com.alexvt.assistant.repository

class AiTextCompleteDaVinciRepository(credentialsRepository: CredentialsRepository) :
    AiTextCompleteRepository(credentialsRepository) {

    override val model = LanguageModel(
        name = "text-davinci-003",
        label = "DaVinci",
        maxTotalTokens = 4096,
        maxResponseTokens = 512,
        usdPerToken = 0.00002,
        timeoutMillis = 60_000,
    )

}
