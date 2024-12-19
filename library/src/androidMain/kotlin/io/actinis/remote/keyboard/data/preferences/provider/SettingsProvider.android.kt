package io.actinis.remote.keyboard.data.preferences.provider

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings


@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class SettingsProviderImpl(
    private val context: Context,
) : SettingsProvider {
    actual override fun createSettings(name: String): Settings {
        return context
            .getSharedPreferences(name, Context.MODE_PRIVATE)
            .let(::SharedPreferencesSettings)
    }
}