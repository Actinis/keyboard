@file:OptIn(ExperimentalResourceApi::class)

package io.actinis.remote.keyboard.util.resources

import io.actinis.remote.library.generated.resources.Res
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi

internal suspend inline fun <reified T> loadJsonFromResources(
    path: String,
    json: Json,
    ioDispatcher: CoroutineDispatcher,
    defaultDispatcher: CoroutineDispatcher,
): T {
    val jsonString = withContext(ioDispatcher) {
        Res.readBytes(path = path).decodeToString()
    }

    return withContext(defaultDispatcher) {
        json.decodeFromString(jsonString)
    }
}