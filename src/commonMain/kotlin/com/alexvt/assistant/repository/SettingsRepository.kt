package com.alexvt.assistant.repository

import com.alexvt.assistant.AppScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.tatarka.inject.annotations.Inject

@kotlinx.serialization.Serializable
data class Settings(
    val isCostEstimatingEnabled: Boolean,
)

@AppScope
@Inject
class SettingsRepository(
    private val storageRepository: StorageRepository
) {
    private val defaultSettings = Settings(
        isCostEstimatingEnabled = true,
    )
    private val storageKey = "settings"
    private val json = Json { prettyPrint = true }

    private val settingsMutableFlow: MutableStateFlow<Settings> =
        MutableStateFlow(
            storageRepository.readEntry(
                key = storageKey,
                defaultValue = json.encodeToString(defaultSettings)
            ).let { jsonString ->
                try {
                    json.decodeFromString(jsonString)
                } catch (t: SerializationException) {
                    // settings migration strategy on schema change: reset to default
                    defaultSettings
                }
            }
        )

    fun watchSettings(): StateFlow<Settings> =
        settingsMutableFlow.asStateFlow()

    fun readSettings(): Settings =
        settingsMutableFlow.value

    suspend fun updateSettings(newSettings: Settings) {
        settingsMutableFlow.emit(newSettings).also {
            storageRepository.writeEntry(key = storageKey, value = json.encodeToString(newSettings))
        }
    }

}
