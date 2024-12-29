package io.actinis.remote.keyboard.data.preferences.provider

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import platform.Foundation.NSUserDefaults

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class SettingsProviderImpl : SettingsProvider {

    actual override fun createSettings(name: String): Settings {
        val delegate = NSUserDefaults(suiteName = name)
        return NSUserDefaultsSettings(delegate)
    }
}