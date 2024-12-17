package io.actinis.remote.keyboard.data.config.model.layout

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GlobalConfig(
    @SerialName("version")
    val version: String,
    @SerialName("defaultLayout")
    val defaultLayout: String,
    @SerialName("availableLayouts")
    val availableLayouts: Map<String, LayoutConfig> = emptyMap(),
    @SerialName("contextRules")
    val contextRules: Map<ContextRule.Type, ContextRule> = emptyMap(),
) {

    @Serializable
    data class LayoutConfig(
        @SerialName("type")
        val type: Type,
        @SerialName("name")
        val name: String,
        @SerialName("variants")
        val variants: List<Variant> = emptyList(),
        @SerialName("requires")
        val requires: List<String> = emptyList(),
    ) {

        @Serializable
        enum class Type {
            @SerialName("alphabetic")
            ALPHABETIC,

            @SerialName("emoji")
            EMOJI,

            @SerialName("numeric")
            NUMERIC,

            @SerialName("symbols")
            SYMBOLS,
        }

        @Serializable
        enum class Variant {
            @SerialName("default")
            DEFAULT,

            @SerialName("email")
            EMAIL,

            @SerialName("url")
            URL,
        }
    }

    @Serializable
    data class ContextRule(
        @SerialName("trigger")
        val trigger: LayoutConfig.Variant,
        @SerialName("extraKeys")
        val extraKeys: List<String> = emptyList(),
    ) {

        @Serializable
        enum class Type {
            @SerialName("email")
            EMAIL,
        }
    }
}