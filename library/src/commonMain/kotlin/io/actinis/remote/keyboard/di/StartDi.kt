package io.actinis.remote.keyboard.di

import io.actinis.remote.keyboard.di.module.*
import org.koin.core.KoinApplication
import org.koin.dsl.KoinAppDeclaration

fun KoinApplication.configureModules() {
    modules(
        coreModule,
        coroutinesModule,
        platformDatabaseModule,
        platformSettingsModule,
        configurationModule,
        languagesModule,
        preferencesModule,
        keyboardModule,
    )
}

fun koinConfiguration(): KoinAppDeclaration = {
    configureModules()
}