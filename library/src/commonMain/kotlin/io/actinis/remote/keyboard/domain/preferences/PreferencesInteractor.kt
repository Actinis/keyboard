package io.actinis.remote.keyboard.domain.preferences

import co.touchlab.kermit.Logger
import io.actinis.remote.keyboard.data.config.model.layout.LayoutType
import io.actinis.remote.keyboard.data.config.repository.KeyboardLayoutsRepository
import io.actinis.remote.keyboard.data.preferences.db.model.EnabledKeyboardLayout
import io.actinis.remote.keyboard.data.preferences.repository.PreferencesRepository
import io.actinis.remote.keyboard.domain.model.preferences.AvailableKeyboardLayoutPreference
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

internal interface PreferencesInteractor {
    val availableKeyboardLayouts: StateFlow<List<AvailableKeyboardLayoutPreference>>

    fun initialize()
    fun updateAvailableKeyboardLayouts(keyboardLayouts: List<AvailableKeyboardLayoutPreference>)
}

internal class PreferencesInteractorImpl(
    private val keyboardLayoutsRepository: KeyboardLayoutsRepository,
    private val preferencesRepository: PreferencesRepository,
    defaultDispatcher: CoroutineDispatcher,
) : PreferencesInteractor {

    private val logger = Logger.withTag(LOG_TAG)

    private val coroutineScope = CoroutineScope(defaultDispatcher + SupervisorJob())

    private val enabledKeyboardLayouts: StateFlow<List<EnabledKeyboardLayout>> = preferencesRepository
        .enabledKeyboardLayouts
        .stateIn(coroutineScope, SharingStarted.Eagerly, emptyList())

    val _availableKeyboardLayouts = MutableStateFlow<List<AvailableKeyboardLayoutPreference>>(emptyList())
    override val availableKeyboardLayouts = _availableKeyboardLayouts.asStateFlow()

    override fun initialize() {
        listenToEnabledKeyboardLayoutsChanges()
    }

    private fun listenToEnabledKeyboardLayoutsChanges() {
        coroutineScope.launch {
            enabledKeyboardLayouts.collect { enabledKeyboardLayouts ->
                logger.d { "Enabled keyboard layouts list changed: $enabledKeyboardLayouts" }

                if (enabledKeyboardLayouts.isEmpty()) {
                    // Enable the default layout if none enabled
                    enableDefaultLayout()
                }

                if (availableKeyboardLayouts.value.isEmpty()) {
                    // Initial update
                    updateActuallyAvailableKeyboardLayouts()
                }
            }
        }
    }

    override fun updateAvailableKeyboardLayouts(keyboardLayouts: List<AvailableKeyboardLayoutPreference>) {
        coroutineScope.launch {
            logger.d { "updateAvailableKeyboardLayouts: keyboardLayouts=$keyboardLayouts" }

            val layoutsToDisable = enabledKeyboardLayouts.value.filter { enabledKeyboardLayout ->
                keyboardLayouts.find { !it.isEnabled && it.id == enabledKeyboardLayout.id } != null
            }

            if (layoutsToDisable.isNotEmpty()) {
                logger.d { "Disabling ${layoutsToDisable.size} keyboard layouts: $layoutsToDisable" }

                preferencesRepository.disableKeyboardLayouts(layoutsToDisable)
            }

            val keyboardLayoutsToUpdate = keyboardLayouts
                .filter { it.isEnabled }
                .map { keyboardLayout ->
                    EnabledKeyboardLayout(
                        id = keyboardLayout.id,
                    )
                }

            if (keyboardLayoutsToUpdate.isNotEmpty()) {
                logger.d { "Updating ${keyboardLayoutsToUpdate.size} keyboard layouts: $keyboardLayoutsToUpdate" }
                preferencesRepository.updateKeyboardLayouts(keyboardLayoutsToUpdate)
            }

            if (keyboardLayouts.isEmpty()) {
                enableDefaultLayout()
            }

            updateActuallyAvailableKeyboardLayouts()
        }
    }

    private suspend fun enableDefaultLayout() {
        logger.d { "enableDefaultLayout" }

        val globalLayoutsConfig = keyboardLayoutsRepository.globalConfig
        val defaultLayout = EnabledKeyboardLayout(
            id = globalLayoutsConfig.defaultLayout,
        )

        logger.d { "Will enable default layout = $defaultLayout" }

        preferencesRepository.updateKeyboardLayouts(listOf(defaultLayout))
    }

    /**
     * Collects info about layouts from preferences, db and configuration and emits as current state
     */
    private fun updateActuallyAvailableKeyboardLayouts() {
        logger.d { "updateActuallyAvailableKeyboardLayouts" }

        val currentlyEnabledKeyboardLayouts = enabledKeyboardLayouts.value

        val globalLayoutsConfig = keyboardLayoutsRepository.globalConfig
        val availableKeyboardLayouts = globalLayoutsConfig.availableLayouts.mapNotNull { (key, layout) ->
            if (layout.type !in availableLayoutsTypes) {
                logger.d { "Ignoring layout of type ${layout.type}" }
                return@mapNotNull null
            }

            val id = "${layout.type}/${key}/default"
            val isEnabled = currentlyEnabledKeyboardLayouts.find { it.id == id } != null

            AvailableKeyboardLayoutPreference(
                id = id,
                name = layout.name,
                isEnabled = isEnabled,
            )
        }

        logger.d { "${availableKeyboardLayouts.size} layouts available: $availableKeyboardLayouts" }

        _availableKeyboardLayouts.value = availableKeyboardLayouts
    }

    private companion object {
        private const val LOG_TAG = "PreferencesInteractor"

        private val availableLayoutsTypes = setOf(
            LayoutType.ALPHABETIC,
            LayoutType.NUMERIC,
        )
    }
}