package io.actinis.remote.keyboard.di.module

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import io.actinis.remote.keyboard.data.preferences.db.db.PreferencesDatabase
import org.koin.dsl.module

internal actual val platformDatabaseModule = module {

    factory<RoomDatabase.Builder<PreferencesDatabase>> {
        val context: Context = get()
        val dbFile = context.getDatabasePath("preferences.db")

        Room.databaseBuilder<PreferencesDatabase>(
            context = context,
            name = dbFile.absolutePath
        )
    }
}