package io.actinis.remote.keyboard.data.preferences.repository

import co.touchlab.kermit.Logger
import io.actinis.remote.keyboard.data.preferences.db.dao.EnabledKeyboardLayoutsDao
import io.actinis.remote.keyboard.data.preferences.db.model.EnabledKeyboardLayout
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow

internal interface PreferencesRepository {
    val enabledKeyboardLayouts: Flow<List<EnabledKeyboardLayout>>

    suspend fun disableKeyboardLayouts(keyboardLayouts: List<EnabledKeyboardLayout>)
    suspend fun updateKeyboardLayouts(keyboardLayouts: List<EnabledKeyboardLayout>)
}

internal class PreferencesRepositoryImpl(
    private val enabledKeyboardLayoutsDao: EnabledKeyboardLayoutsDao,
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

    private companion object {
        private const val LOG_TAG = "PreferencesRepository"
    }
}