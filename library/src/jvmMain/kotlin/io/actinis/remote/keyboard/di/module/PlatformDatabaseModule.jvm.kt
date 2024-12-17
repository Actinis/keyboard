package io.actinis.remote.keyboard.di.module

import androidx.room.Room
import androidx.room.RoomDatabase
import io.actinis.remote.keyboard.data.preferences.db.db.PreferencesDatabase
import io.actinis.remote.keyboard.util.getDataDirectory
import org.koin.dsl.module
import java.io.File

internal actual val platformDatabaseModule = module {

    factory<RoomDatabase.Builder<PreferencesDatabase>> {
        val dbFile = File(getDataDirectory(), "remote_apps.db")

        Room.databaseBuilder<PreferencesDatabase>(
            name = dbFile.absolutePath,
        )
    }
}