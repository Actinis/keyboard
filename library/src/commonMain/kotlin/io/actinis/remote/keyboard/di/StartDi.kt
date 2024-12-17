package io.actinis.remote.keyboard.di

import io.actinis.remote.keyboard.di.module.*
import org.koin.core.KoinApplication
import org.koin.dsl.KoinAppDeclaration

fun KoinApplication.configureModules() {
    modules(
        coreModule,
        coroutinesModule,
        platformDatabaseModule,
        configurationModule,
        preferencesModule,
        keyboardModule,
    )
}

fun koinConfiguration(): KoinAppDeclaration = {
    configureModules()
}