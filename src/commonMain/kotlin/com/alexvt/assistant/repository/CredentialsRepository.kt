package com.alexvt.assistant.repository

import com.alexvt.assistant.AppScope
import me.tatarka.inject.annotations.Inject
import java.io.File

@AppScope
@Inject
class CredentialsRepository() {

    fun getOpenAiBearerToken(): String {
        return File("${System.getProperty("user.home")}/.openai-credentials").readText().trim()
    }
}
