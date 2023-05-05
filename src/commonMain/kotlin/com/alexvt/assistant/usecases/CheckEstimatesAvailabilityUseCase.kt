package com.alexvt.assistant.usecases

import com.alexvt.assistant.AppScope
import com.alexvt.assistant.repository.SettingsRepository
import me.tatarka.inject.annotations.Inject

@AppScope
@Inject
class CheckEstimatesAvailabilityUseCase(
    private val settingsRepository: SettingsRepository,
) {

    fun execute(): Boolean =
        settingsRepository.readSettings().isCostEstimatingEnabled

}
