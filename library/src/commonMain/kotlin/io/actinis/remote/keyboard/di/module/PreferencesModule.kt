package io.actinis.remote.keyboard.di.module

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.actinis.remote.keyboard.data.preferences.db.db.PreferencesDatabase
import io.actinis.remote.keyboard.data.preferences.provider.SettingsProvider
import io.actinis.remote.keyboard.data.preferences.repository.PreferencesRepository
import io.actinis.remote.keyboard.data.preferences.repository.PreferencesRepositoryImpl
import io.actinis.remote.keyboard.di.name.DatabasesNames
import io.actinis.remote.keyboard.di.name.DispatchersNames
import io.actinis.remote.keyboard.di.name.SettingsNames
import io.actinis.remote.keyboard.domain.preferences.PreferencesInteractor
import io.actinis.remote.keyboard.domain.preferences.PreferencesInteractorImpl
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

internal val preferencesModule = module {

    single {
        val builder: RoomDatabase.Builder<PreferencesDatabase> = get((named(DatabasesNames.PREFERENCES)))
        val ioDispatcher: CoroutineDispatcher = get(named(DispatchersNames.IO))

        builder
//            .addMigrations(MIGRATIONS) // FIXME
            .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(ioDispatcher)
            .build()
    }

    single {
        val db: PreferencesDatabase = get()

        db.enabledKeyboardLayoutsDao()
    }

    single(named(SettingsNames.KEYBOARD_LAYOUTS)) {
        val settingsProvider: SettingsProvider = get()
        settingsProvider.createSettings("keyboard_layouts")
    }

    single {
        PreferencesRepositoryImpl(
            enabledKeyboardLayoutsDao = get(),
            keyboardLayoutsSettings = get(named(SettingsNames.KEYBOARD_LAYOUTS)),
            ioDispatcher = get(named(DispatchersNames.IO)),
        )
    } bind PreferencesRepository::class

    single {
        PreferencesInteractorImpl(
            keyboardLayoutsRepository = get(),
            preferencesRepository = get(),
            defaultDispatcher = get(named(DispatchersNames.DEFAULT)),
        )
    } bind PreferencesInteractor::class
}