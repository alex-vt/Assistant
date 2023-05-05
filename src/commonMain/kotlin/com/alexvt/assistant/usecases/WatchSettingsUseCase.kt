package com.alexvt.assistant.usecases

import com.alexvt.assistant.AppScope
import com.alexvt.assistant.repository.Settings
import com.alexvt.assistant.repository.SettingsRepository
import kotlinx.coroutines.flow.StateFlow
import me.tatarka.inject.annotations.Inject

@AppScope
@Inject
class WatchSettingsUseCase(
    private val settingsRepository: SettingsRepository,
) {

    fun execute(): StateFlow<Settings> =
        settingsRepository.watchSettings()

}
