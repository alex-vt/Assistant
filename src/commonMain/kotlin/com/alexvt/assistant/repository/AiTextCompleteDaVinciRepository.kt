package com.alexvt.assistant.repository

import com.alexvt.assistant.AppScope
import me.tatarka.inject.annotations.Inject

@AppScope
@Inject
class AiTextCompleteDaVinciRepository(credentialsRepository: CredentialsRepository) :
    AiTextCompleteRepository(credentialsRepository) {

    override val model = LanguageModel(
        name = "text-davinci-003",
        label = "DaVinci",
        maxTotalTokens = 4096,
        maxResponseTokens = 512,
        usdPerRequestToken = 0.00002,
        usdPerResponseToken = 0.00002,
        timeoutMillis = 60_000,
    )

}
