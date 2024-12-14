package io.actinis.remote.keyboard.di

import io.actinis.remote.keyboard.di.module.configurationModule
import io.actinis.remote.keyboard.di.module.coreModule
import io.actinis.remote.keyboard.di.module.coroutinesModule
import io.actinis.remote.keyboard.di.module.keyboardModule
import org.koin.core.KoinApplication
import org.koin.dsl.KoinAppDeclaration

fun KoinApplication.configureModules() {
    modules(
        coreModule,
        coroutinesModule,
        configurationModule,
        keyboardModule,
    )
}

fun koinConfiguration(): KoinAppDeclaration = {
    configureModules()
}