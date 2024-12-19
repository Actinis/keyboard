package io.actinis.remote.keyboard.data.preferences.provider

import com.russhwolf.settings.Settings

interface SettingsProvider {
    fun createSettings(name: String): Settings
}

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class SettingsProviderImpl : SettingsProvider {
    override fun createSettings(name: String): Settings
}