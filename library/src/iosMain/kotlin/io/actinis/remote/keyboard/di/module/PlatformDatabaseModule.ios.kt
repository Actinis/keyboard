@file:OptIn(ExperimentalForeignApi::class)

package io.actinis.remote.keyboard.di.module

import androidx.room.Room
import androidx.room.RoomDatabase
import io.actinis.remote.keyboard.data.preferences.db.db.PreferencesDatabase
import io.actinis.remote.keyboard.di.name.DatabasesNames
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.qualifier.named
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

private fun documentDirectory(): String {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    return requireNotNull(documentDirectory?.path)
}

internal actual val platformDatabaseModule = module {

    factory<RoomDatabase.Builder<PreferencesDatabase>>(named(DatabasesNames.PREFERENCES)) {
        val dbFilePath = documentDirectory() + "/preferences.db"
        Room.databaseBuilder<PreferencesDatabase>(
            name = dbFilePath,
        )
    }
}