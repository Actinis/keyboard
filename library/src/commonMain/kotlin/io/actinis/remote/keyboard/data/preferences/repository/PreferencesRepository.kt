package io.actinis.remote.keyboard.data.preferences.repository

import co.touchlab.kermit.Logger
import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import io.actinis.remote.keyboard.data.preferences.db.dao.EnabledKeyboardLayoutsDao
import io.actinis.remote.keyboard.data.preferences.db.model.EnabledKeyboardLayout
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow

internal interface PreferencesRepository {
    val enabledKeyboardLayouts: Flow<List<EnabledKeyboardLayout>>

    suspend fun disableKeyboardLayouts(keyboardLayouts: List<EnabledKeyboardLayout>)
    suspend fun updateKeyboardLayouts(keyboardLayouts: List<EnabledKeyboardLayout>)

    suspend fun getLastKeyboardLayoutId(): String?
    suspend fun setLastKeyboardLayoutId(id: String?)
}

internal class PreferencesRepositoryImpl(
    private val enabledKeyboardLayoutsDao: EnabledKeyboardLayoutsDao,
    private val keyboardLayoutsSettings: Settings,
    private val ioDispatcher: CoroutineDispatcher,
) : PreferencesRepository {

    private val logger = Logger.withTag(LOG_TAG)

    override val enabledKeyboardLayouts = enabledKeyboardLayoutsDao.all()

    override suspend fun disableKeyboardLayouts(keyboardLayouts: List<EnabledKeyboardLayout>) {
        logger.d { "disableKeyboardLayouts: keyboardLayouts=$keyboardLayouts" }

        TODO("Not yet implemented")
    }

    override suspend fun updateKeyboardLayouts(keyboardLayouts: List<EnabledKeyboardLayout>) {
        logger.d { "updateKeyboardLayouts: keyboardLayouts=$keyboardLayouts" }

        keyboardLayouts
            .mapIndexed { index, keyboardLayout ->
                if (keyboardLayout.ordering == index) keyboardLayout else keyboardLayout.copy(ordering = index)
            }
            .let { enabledKeyboardLayouts ->
                enabledKeyboardLayoutsDao.upsert(enabledKeyboardLayouts)
            }
    }

    override suspend fun getLastKeyboardLayoutId(): String? {
        return keyboardLayoutsSettings[KEY_LAST_KEYBOARD_LAYOUT_ID]
    }

    override suspend fun setLastKeyboardLayoutId(id: String?) {
        if (id != null) {
            keyboardLayoutsSettings[KEY_LAST_KEYBOARD_LAYOUT_ID] = id
        } else {
            keyboardLayoutsSettings.remove(KEY_LAST_KEYBOARD_LAYOUT_ID)
        }
    }

    private companion object {
        private const val LOG_TAG = "PreferencesRepository"

        private const val KEY_LAST_KEYBOARD_LAYOUT_ID = "last_keyboard_layout_id"
    }
}