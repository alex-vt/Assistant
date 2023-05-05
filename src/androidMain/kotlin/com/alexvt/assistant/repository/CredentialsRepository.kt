package com.alexvt.assistant.repository

import com.alexvt.assistant.AppScope
import com.alexvt.assistant.BuildConfig
import me.tatarka.inject.annotations.Inject

@AppScope
@Inject
actual class CredentialsRepository {

    actual fun getOpenAiBearerToken(): String {
        return BuildConfig.OPENAI_API_KEY
    }
}
