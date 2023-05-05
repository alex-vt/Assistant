package com.alexvt.assistant

import com.alexvt.assistant.repository.CredentialsRepository
import com.alexvt.assistant.repository.ExtractableImageTextRepository
import com.alexvt.assistant.repository.SoundRecordingRepository
import com.alexvt.assistant.repository.StorageRepository
import com.alexvt.assistant.ui.AssistantViewModelUseCases
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import me.tatarka.inject.annotations.Scope

@Scope
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
annotation class AppScope

@AppScope
@Component
abstract class AppDependencies {

    abstract val assistantViewModelUseCases: AssistantViewModelUseCases

    @AppScope
    @Provides
    protected fun credentialsRepository(): CredentialsRepository =
        CredentialsRepository()

    @AppScope
    @Provides
    protected fun soundRecordingRepository(): SoundRecordingRepository =
        SoundRecordingRepository()

    @AppScope
    @Provides
    protected fun extractableImageTextRepository(): ExtractableImageTextRepository =
        ExtractableImageTextRepository()

    @AppScope
    @Provides
    protected fun storageRepository(): StorageRepository =
        StorageRepository()

}
