package io.actinis.remote.keyboard.di.module

import io.actinis.remote.keyboard.domain.text.suggestion.TextSuggestionsInteractor
import io.actinis.remote.keyboard.domain.text.suggestion.TextSuggestionsInteractorImpl
import org.koin.dsl.bind
import org.koin.dsl.module

actual val platformSuggestionsModule = module {

    factory {
        TextSuggestionsInteractorImpl()
    } bind TextSuggestionsInteractor::class

}