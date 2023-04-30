package com.alexvt.assistant.repository

class AiTextChatGpt4Repository(credentialsRepository: CredentialsRepository) :
    AiTextChatRepository(credentialsRepository) {

    override val model = LanguageModel(
        name = "gpt-4",
        label = "GPT4",
        maxTotalTokens = 8192,
        maxResponseTokens = 1024,
        usdPerRequestToken = 0.00003,
        usdPerResponseToken = 0.00006,
        timeoutMillis = 120_000,
    )

}
