package com.alexvt.assistant.repository

import com.alexvt.assistant.AppScope
import me.tatarka.inject.annotations.Inject

@AppScope
@Inject
expect class CredentialsRepository() {

    fun getOpenAiBearerToken(): String
}
