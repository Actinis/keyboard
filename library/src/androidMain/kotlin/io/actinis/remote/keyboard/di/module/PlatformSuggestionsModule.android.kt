package io.actinis.remote.keyboard.di.module

import android.content.Context
import android.view.textservice.TextServicesManager
import io.actinis.remote.keyboard.domain.text.suggestion.TextSuggestionsInteractor
import io.actinis.remote.keyboard.domain.text.suggestion.TextSuggestionsInteractorImpl
import org.koin.dsl.bind
import org.koin.dsl.module

actual val platformSuggestionsModule = module {

    single {
        val context: Context = get()

        context.getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE) as TextServicesManager
    }

    single {
        TextSuggestionsInteractorImpl(
            textServicesManager = get(),
        )
    } bind TextSuggestionsInteractor::class
}