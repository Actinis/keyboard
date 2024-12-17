@file:OptIn(ExperimentalResourceApi::class)

package io.actinis.remote.keyboard.data.config.repository

import co.touchlab.kermit.Logger
import io.actinis.remote.keyboard.data.config.model.layout.GlobalConfig
import io.actinis.remote.keyboard.data.config.model.layout.KeyboardLayout
import io.actinis.remote.library.generated.resources.Res
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi

internal interface ConfigurationRepository {
    val globalConfig: GlobalConfig

    suspend fun initialize()
    suspend fun getLayout(layoutId: String): KeyboardLayout
}

internal class ConfigurationRepositoryImpl(
    private val json: Json,
    private val defaultDispatcher: CoroutineDispatcher,
    private val ioDispatcher: CoroutineDispatcher,
) : ConfigurationRepository {

    private val logger = Logger.withTag(LOG_TAG)

    private var _globalConfig: GlobalConfig? = null

    override val globalConfig: GlobalConfig
        get() = requireNotNull(_globalConfig) { "Initialize configuration first" }

    private val layouts = mutableMapOf<String, KeyboardLayout>()

    override suspend fun initialize() {
        logger.i { "Initializing" }

        withContext(defaultDispatcher) {
            _globalConfig = loadJsonFromResources(path = GLOBAL_CONFIG_PATH)

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

        val (type, name, variant) = layoutId.split("/")

        logger.d { "loadLayout: type=$type, name=$name, variant=$variant" }

        val layoutConfig = globalConfig.availableLayouts[name]
            ?: throw IllegalArgumentException("Unknown layout id = $layoutId")

        logger.d { "layout config: $layoutConfig" }

        val filePath = String.format(
            format = LAYOUT_DEFAULT_FILE_PATH_FORMAT,
            layoutId,
        )

        return loadJsonFromResources<KeyboardLayout>(path = filePath)
    }

    private suspend inline fun <reified T> loadJsonFromResources(path: String): T {
        val jsonString = withContext(ioDispatcher) {
            Res.readBytes(path = path).decodeToString()
        }

        return withContext(defaultDispatcher) {
            json.decodeFromString(jsonString)
        }
    }

    private companion object {
        private const val LOG_TAG = "ConfigurationRepository"

        private const val GLOBAL_CONFIG_PATH = "files/keyboards/config/layouts.json"

        private const val LAYOUT_DEFAULT_FILE_PATH_FORMAT = "files/keyboards/layouts/%s.json"
    }
}