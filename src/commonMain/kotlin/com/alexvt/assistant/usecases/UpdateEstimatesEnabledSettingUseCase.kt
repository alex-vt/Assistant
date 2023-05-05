package com.alexvt.assistant.usecases

import com.alexvt.assistant.AppScope
import com.alexvt.assistant.repository.SettingsRepository
import me.tatarka.inject.annotations.Inject

@AppScope
@Inject
class UpdateEstimatesEnabledSettingUseCase(
    private val settingsRepository: SettingsRepository,
) {

    suspend fun execute(newValue: Boolean) {
        with(settingsRepository) {
            updateSettings(readSettings().copy(isCostEstimatingEnabled = newValue))
        }
    }

}
