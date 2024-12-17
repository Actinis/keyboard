package io.actinis.remote.keyboard.data.preferences.db.dao

import androidx.room.*
import io.actinis.remote.keyboard.data.preferences.db.model.EnabledKeyboardLayout
import kotlinx.coroutines.flow.Flow

@Dao
internal interface EnabledKeyboardLayoutsDao {

    @Query("SELECT * FROM enabled_keyboard_layouts ORDER BY ordering ASC")
    fun all(): Flow<List<EnabledKeyboardLayout>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(enabledKeyboardLayout: EnabledKeyboardLayout)

    @Upsert
    suspend fun upsert(enabledKeyboardLayouts: List<EnabledKeyboardLayout>)

    @Delete
    suspend fun delete(enabledKeyboardLayouts: List<EnabledKeyboardLayout>)
}