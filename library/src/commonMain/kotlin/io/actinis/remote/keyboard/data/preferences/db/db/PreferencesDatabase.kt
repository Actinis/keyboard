package io.actinis.remote.keyboard.data.preferences.db.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import io.actinis.remote.keyboard.data.preferences.db.dao.EnabledKeyboardLayoutsDao
import io.actinis.remote.keyboard.data.preferences.db.model.EnabledKeyboardLayout

@Suppress("NO_ACTUAL_FOR_EXPECT", "EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
internal expect object PreferencesDatabaseConstructor : RoomDatabaseConstructor<PreferencesDatabase> {
    override fun initialize(): PreferencesDatabase
}

@Database(
    entities = [
        EnabledKeyboardLayout::class,
    ],
    version = 1,
)
@ConstructedBy(PreferencesDatabaseConstructor::class)
internal abstract class PreferencesDatabase : RoomDatabase() {

    abstract fun enabledKeyboardLayoutsDao(): EnabledKeyboardLayoutsDao
}