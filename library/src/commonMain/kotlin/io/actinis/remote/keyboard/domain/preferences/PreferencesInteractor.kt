package io.actinis.remote.keyboard.domain.preferences

import co.touchlab.kermit.Logger
import io.actinis.remote.keyboard.data.config.model.layout.GlobalConfig
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

    suspend fun getLastKeyboardLayoutId(): String?
    suspend fun setLastKeyboardLayoutId(id: String?)
}

internal class PreferencesInteractorImpl(
    private val keyboardLayoutsRepository: KeyboardLayoutsRepository,
    private val preferencesRepository: PreferencesRepository,
    defaultDispatcher: CoroutineDispatcher,
) : PreferencesInteractor {

    private val logger = Logger.withTag(LOG_TAG)

    private val coroutineScope = CoroutineScope(defaultDispatcher + SupervisorJob())

    private val enabledKeyboardLayouts: StateFlow<List<EnabledKeyboardLayout>?> = preferencesRepository
        .enabledKeyboardLayouts
        .stateIn(coroutineScope, SharingStarted.Eagerly, null)

    private val _availableKeyboardLayouts = MutableStateFlow<List<AvailableKeyboardLayoutPreference>>(emptyList())
    override val availableKeyboardLayouts = _availableKeyboardLayouts.asStateFlow()

    override fun initialize() {
        listenToEnabledKeyboardLayoutsChanges()
    }

    private fun listenToEnabledKeyboardLayoutsChanges() {
        coroutineScope.launch {
            enabledKeyboardLayouts
                .filterNotNull()
                .onEach { enabledKeyboardLayouts ->
                    logger.d { "Enabled keyboard layouts list changed: $enabledKeyboardLayouts" }

                    if (enabledKeyboardLayouts.isEmpty()) {
                        // Enable the default layout if none enabled
                        enableDefaultLayout()
                        return@onEach // Skip further processing until we get updated state
                    }

                    if (availableKeyboardLayouts.value.isEmpty()) {
                        // Initial update
                        updateActuallyAvailableKeyboardLayouts()
                    }
                }
                .collect()
        }
    }

    override fun updateAvailableKeyboardLayouts(keyboardLayouts: List<AvailableKeyboardLayoutPreference>) {
        coroutineScope.launch {
            logger.d { "updateAvailableKeyboardLayouts: keyboardLayouts=$keyboardLayouts" }

            val layoutsToDisable = enabledKeyboardLayouts.value.orEmpty().filter { enabledKeyboardLayout ->
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

    /**
     * Enables the default layout
     * Currently there is no layouts management, we always enable en_US, ru_RU and Emoji keyboards by default
     * So for now it simply enables everything
     */
    private suspend fun enableDefaultLayout() {
        logger.d { "enableDefaultLayout" }

        // TODO: Uncomment when layout management is ready
//        val globalLayoutsConfig = keyboardLayoutsRepository.globalConfig
//        val defaultLayout = EnabledKeyboardLayout(
//            id = globalLayoutsConfig.defaultLayout,
//        )
//
//        logger.d { "Will enable default layout = $defaultLayout" }
//        preferencesRepository.updateKeyboardLayouts(listOf(defaultLayout))

        keyboardLayoutsRepository.globalConfig.availableLayouts
            .filter { (_, layoutConfig) ->
                layoutConfig.type in availableLayoutsTypes
            }
            .map { (key, layoutConfig) ->
                EnabledKeyboardLayout(
                    id = getFullLayoutId(layoutConfig = layoutConfig, key = key)
                )
            }
            .let { enabledKeyboardLayouts ->
                preferencesRepository.updateKeyboardLayouts(enabledKeyboardLayouts)
            }
    }

    /**
     * Collects info about layouts from preferences, db and configuration and emits as current state
     */
    private fun updateActuallyAvailableKeyboardLayouts() {
        logger.d { "updateActuallyAvailableKeyboardLayouts" }

        val currentlyEnabledKeyboardLayouts = enabledKeyboardLayouts.value

        logger.d { "${currentlyEnabledKeyboardLayouts?.size ?: 0} enabled keyboard layouts: $currentlyEnabledKeyboardLayouts" }

        val globalLayoutsConfig = keyboardLayoutsRepository.globalConfig
        val availableKeyboardLayouts = globalLayoutsConfig.availableLayouts.mapNotNull { (key, layoutConfig) ->
            if (layoutConfig.type !in availableLayoutsTypes) {
                logger.d { "Ignoring layout of type ${layoutConfig.type}" }
                return@mapNotNull null
            }

            val id = getFullLayoutId(layoutConfig = layoutConfig, key = key)
            val isEnabled = currentlyEnabledKeyboardLayouts?.find { it.id == id } != null

            AvailableKeyboardLayoutPreference(
                id = id,
                name = layoutConfig.name,
                isEnabled = isEnabled,
            )
        }

        logger.d { "${availableKeyboardLayouts.size} layouts available: $availableKeyboardLayouts" }

        _availableKeyboardLayouts.value = availableKeyboardLayouts
    }

    override suspend fun getLastKeyboardLayoutId(): String? {
        return preferencesRepository.getLastKeyboardLayoutId()
    }

    override suspend fun setLastKeyboardLayoutId(id: String?) {
        preferencesRepository.setLastKeyboardLayoutId(id = id)
    }

    private fun getFullLayoutId(layoutConfig: GlobalConfig.LayoutConfig, key: String): String {
        return "${layoutConfig.type}/${key}/default"
    }

    private companion object {
        private const val LOG_TAG = "PreferencesInteractor"

        private val availableLayoutsTypes = setOf(
            LayoutType.ALPHABETIC,
            LayoutType.EMOJI,
        )
    }
}