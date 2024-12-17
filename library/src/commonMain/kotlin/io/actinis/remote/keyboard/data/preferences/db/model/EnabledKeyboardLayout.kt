package io.actinis.remote.keyboard.data.preferences.db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Keyboard layout that is enabled by user in the preferences
 */
@Entity(
    tableName = "enabled_keyboard_layouts",
    indices = [
        Index(
            value = ["ordering"],
            unique = true,
            orders = [Index.Order.ASC],
        )
    ],
)
internal data class EnabledKeyboardLayout(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "ordering")
    val ordering: Int = 0,
)
