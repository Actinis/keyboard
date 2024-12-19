package io.actinis.remote.keyboard.di.module

import io.actinis.remote.keyboard.data.preferences.provider.SettingsProvider
import io.actinis.remote.keyboard.data.preferences.provider.SettingsProviderImpl
import org.koin.dsl.bind
import org.koin.dsl.module

actual val platformSettingsModule = module {
    factory { SettingsProviderImpl() } bind SettingsProvider::class
}