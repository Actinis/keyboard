package io.actinis.remote.keyboard.data.config.repository

import co.touchlab.kermit.Logger
import io.actinis.remote.keyboard.data.config.model.layout.GlobalConfig
import io.actinis.remote.keyboard.data.config.model.layout.KeyboardLayout
import io.actinis.remote.keyboard.util.resources.loadJsonFromResources
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

internal interface KeyboardLayoutsRepository {
    val globalConfig: GlobalConfig

    suspend fun initialize()
    suspend fun getLayout(layoutId: String): KeyboardLayout
}

internal class KeyboardLayoutsRepositoryImpl(
    private val json: Json,
    private val defaultDispatcher: CoroutineDispatcher,
    private val ioDispatcher: CoroutineDispatcher,
) : KeyboardLayoutsRepository {

    private val logger = Logger.withTag(LOG_TAG)

    private var _globalConfig: GlobalConfig? = null

    override val globalConfig: GlobalConfig
        get() = requireNotNull(_globalConfig) { "Initialize configuration first" }

    private val layouts = mutableMapOf<String, KeyboardLayout>()

    override suspend fun initialize() {
        logger.i { "Initializing" }

        withContext(defaultDispatcher) {
            _globalConfig = loadJsonFromResources(
                path = GLOBAL_CONFIG_PATH,
                json = json,
                ioDispatcher = ioDispatcher,
                defaultDispatcher = defaultDispatcher,
            )

            logger.i {
                "Initialized, version=${globalConfig.version}, ${globalConfig.availableLayouts.size} layouts, " +
                        "default layout = ${globalConfig.defaultLayout}"
            }
        }
    }

    override suspend fun getLayout(layoutId: String): KeyboardLayout {
        logger.d { "getLayout: layoutId=$layoutId" }

        var layout = layouts[layoutId]
        if (layout == null) {
            layout = loadLayout(layoutId = layoutId)
            layouts[layoutId] = layout
        }

        return layout
    }

    private suspend fun loadLayout(layoutId: String): KeyboardLayout {
        logger.d { "loadLayout: layoutId=$layoutId" }

        val (type, key, variant) = layoutId.split("/")

        logger.d { "loadLayout: type=$type, key=$key, variant=$variant" }

        val layoutConfig = globalConfig.availableLayouts[key]
            ?: throw IllegalArgumentException("Unknown layoutId = $layoutId")

        logger.d { "layout config: $layoutConfig" }

        val filePath = "files/keyboards/layouts/$layoutId.json"

        return loadJsonFromResources<KeyboardLayout>(
            path = filePath,
            json = json,
            ioDispatcher = ioDispatcher,
            defaultDispatcher = defaultDispatcher,
        )
    }

    private companion object {
        private const val LOG_TAG = "ConfigurationRepository"

        private const val GLOBAL_CONFIG_PATH = "files/keyboards/config/layouts.json"

        private const val LAYOUT_DEFAULT_FILE_PATH_FORMAT = "files/keyboards/layouts/%s.json"
    }
}