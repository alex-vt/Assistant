package com.alexvt.assistant.repository

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

actual class StorageRepository {

    private val homeDirectory = System.getProperty("user.home")
    private val appFolderName = ".Assistant"

    private fun getEntryPath(key: String): Path =
        Paths.get(homeDirectory, appFolderName, "$key.json")

    actual fun readEntry(key: String, defaultValue: String): String {
        return with(getEntryPath(key)) {
            if (Files.exists(this)) {
                Files.readString(this)
            } else {
                defaultValue
            }
        }
    }

    actual fun writeEntry(key: String, value: String) {
        with(getEntryPath(key)) {
            Files.createDirectories(parent)
            Files.writeString(this, value)
        }
    }

}