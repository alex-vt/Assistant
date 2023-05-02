package com.alexvt.assistant.repository

import com.alexvt.assistant.AppScope
import me.tatarka.inject.annotations.Inject

@AppScope
@Inject
class AiTextChatGptTurboRepository(credentialsRepository: CredentialsRepository) :
    AiTextChatRepository(credentialsRepository) {

    override val model = LanguageModel(
        name = "gpt-3.5-turbo",
        label = "Turbo",
        maxTotalTokens = 4096,
        maxResponseTokens = 512,
        usdPerRequestToken = 0.000002,
        usdPerResponseToken = 0.000002,
        timeoutMillis = 60_000,
    )

}
