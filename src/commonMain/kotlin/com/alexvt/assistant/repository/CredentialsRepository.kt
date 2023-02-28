package com.alexvt.assistant.repository

import java.io.File

class CredentialsRepository {

    fun getOpenAiBearerToken(): String {
        return File("${System.getProperty("user.home")}/.openai-credentials").readText().trim()
    }
}
