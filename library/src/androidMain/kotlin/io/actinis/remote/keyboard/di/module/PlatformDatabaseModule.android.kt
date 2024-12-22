package io.actinis.remote.keyboard.di.module

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import io.actinis.remote.keyboard.data.preferences.db.db.PreferencesDatabase
import io.actinis.remote.keyboard.di.name.DatabasesNames
import org.koin.core.qualifier.named
import org.koin.dsl.module

internal actual val platformDatabaseModule = module {

    factory<RoomDatabase.Builder<PreferencesDatabase>>(named(DatabasesNames.PREFERENCES)) {
        val context: Context = get()
        val dbFile = context.getDatabasePath("preferences.db")

        Room.databaseBuilder<PreferencesDatabase>(
            context = context,
            name = dbFile.absolutePath
        )
    }
}