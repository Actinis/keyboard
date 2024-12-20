package io.actinis.remote.keyboard.di.module

import androidx.compose.foundation.MutatePriority
import io.actinis.remote.keyboard.data.text.repository.LanguagesRepository
import io.actinis.remote.keyboard.data.text.repository.LanguagesRepositoryImpl
import io.actinis.remote.keyboard.di.name.DispatchersNames
import io.actinis.remote.keyboard.domain.text.TextAnalyzer
import io.actinis.remote.keyboard.domain.text.TextAnalyzerImpl
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

internal val languagesModule = module {

    single {
        LanguagesRepositoryImpl(
            json = get(),
            ioDispatcher = get(named(DispatchersNames.IO)),
            defaultDispatcher = get(named(MutatePriority.Default)),
        )
    } bind LanguagesRepository::class

    single {
        TextAnalyzerImpl(
            languagesRepository = get(),
        )
    } bind TextAnalyzer::class
}