package io.actinis.remote.keyboard.di

import io.actinis.remote.keyboard.di.module.*
import org.koin.core.KoinApplication
import org.koin.dsl.KoinAppDeclaration

val keyboardModules = arrayOf(
    coreModule,
    coroutinesModule,
    platformDatabaseModule,
    platformSettingsModule,
    configurationModule,
    languagesModule,
    platformSuggestionsModule,
    preferencesModule,
    keyboardModule,
)

fun KoinApplication.configureModules() {
    modules(*keyboardModules)
}

fun koinConfiguration(): KoinAppDeclaration = {
    configureModules()
}