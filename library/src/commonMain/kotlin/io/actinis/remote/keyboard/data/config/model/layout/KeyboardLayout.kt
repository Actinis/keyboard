package io.actinis.remote.keyboard.data.config.model.layout

import io.actinis.remote.keyboard.data.config.model.behavior.Behaviors
import io.actinis.remote.keyboard.data.config.model.key.Key
import io.actinis.remote.keyboard.data.config.model.key.KeysRow
import io.actinis.remote.keyboard.data.config.model.visual.FontSize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KeyboardLayout(
    @SerialName("metadata")
    val metadata: Metadata,
    @SerialName("dimensions")
    val dimensions: Dimensions,
    @SerialName("rows")
    val rows: List<KeysRow> = emptyList(),
    @SerialName("variants")
    val variants: List<Variant> = emptyList(),
    @SerialName("popups")
    val popups: Popups,
    @SerialName("behaviors")
    val behaviors: Behaviors,
    @SerialName("visual")
    val visual: Visual,
) {

    fun findKey(predicate: (key: Key) -> Boolean): Key? {
        rows.forEach { row ->
            row.keys.find(predicate)?.let { return it }
        }

        return null
    }

    @Serializable
    data class Metadata(
        @SerialName("id")
        val id: String,
        @SerialName("name")
        val name: String,
        @SerialName("version")
        val version: String,
        @SerialName("type")
        val type: String,
        @SerialName("inheritsFrom")
        val inheritsFrom: String? = null,
    )

    @Serializable
    data class Dimensions(
        @SerialName("rowCount")
        val rowCount: Int,
        @SerialName("defaultKeyWidth")
        val defaultKeyWidth: Float,
        @SerialName("defaultKeyHeight")
        val defaultKeyHeight: Float,
        @SerialName("baseKeyWidthMultiplier")
        val baseKeyWidthMultiplier: Float,
        @SerialName("baseKeyHeightToWidthMultiplier")
        val baseKeyHeightToWidthMultiplier: Float,
        @SerialName("columnsSpacingMultiplier")
        val columnsSpacingMultiplier: Float,
        @SerialName("rowsSpacingToHeightMultiplier")
        val rowsSpacingToHeightMultiplier: Float,
    )

    @Serializable
    data class Variant(
        @SerialName("type")
        val type: String,
        @SerialName("insertAt")
        val insertAt: InsertPosition,
    ) {
        @Serializable
        data class InsertPosition(
            @SerialName("row")
            val row: Int,
            @SerialName("position")
            val position: Int,
            @SerialName("key")
            val key: Key,
        )
    }

    @Serializable
    data class Popups(
        @SerialName("enabled")
        val enabled: Boolean,
        @SerialName("delay")
        val delay: Int,
        @SerialName("style")
        val style: String,
    )

    @Serializable
    data class Visual(
        @SerialName("fontSize")
        val fontSize: FontSize,
        @SerialName("popupScale")
        val popupScale: Double,
    )
}