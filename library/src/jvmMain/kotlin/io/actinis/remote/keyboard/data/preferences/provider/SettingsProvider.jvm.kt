package io.actinis.remote.keyboard.data.preferences.provider

import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import java.util.prefs.Preferences

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class SettingsProviderImpl : SettingsProvider {

    actual override fun createSettings(name: String): Settings {
        return Preferences
            .userRoot()
            .node("io/actinis/remote/$name")
            .let(::PreferencesSettings)
    }
}