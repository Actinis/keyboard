package io.actinis.remote.keyboard.data.state.model

sealed interface InputState {
    val text: String
    val selectionStart: Int
    val selectionEnd: Int
    val isMultiline: Boolean
    val actionType: ActionType
    val isPersonalizedLearningEnabled: Boolean

    data class Text(
        override val text: String,
        override val selectionStart: Int,
        override val selectionEnd: Int,
        override val isMultiline: Boolean = false,
        override val actionType: ActionType = ActionType.NONE,
        override val isPersonalizedLearningEnabled: Boolean = true,
        val isAutoComplete: Boolean = false,
        val isAutoCorrect: Boolean = false,
        val capitalizationMode: CapitalizationMode = CapitalizationMode.NONE,
        val variation: Variation = Variation.NORMAL,
    ) : InputState {

        enum class CapitalizationMode {
            NONE,
            ALL_CHARACTERS,
            WORDS,
            SENTENCES,
        }

        enum class Variation {
            NORMAL,
            EMAIL_ADDRESS,
            EMAIL_SUBJECT,
            FILTER,
            LONG_MESSAGE,
            PASSWORD,
            PERSON_NAME,
            PHONETIC,
            POSTAL_ADDRESS,
            SHORT_MESSAGE,
            URI,
        }
    }

    data class Number(
        override val text: String,
        override val selectionStart: Int,
        override val selectionEnd: Int,
        override val isMultiline: Boolean = false,
        override val actionType: ActionType = ActionType.NONE,
        override val isPersonalizedLearningEnabled: Boolean = true,
        val isSigned: Boolean = false,
        val isDecimal: Boolean = false,
        val variation: Variation = Variation.NORMAL,
    ) : InputState {

        enum class Variation {
            NORMAL,
            PASSWORD,
        }
    }

    data class Phone(
        override val text: String,
        override val selectionStart: Int,
        override val selectionEnd: Int,
        override val isMultiline: Boolean = false,
        override val actionType: ActionType = ActionType.NONE,
        override val isPersonalizedLearningEnabled: Boolean = true,
    ) : InputState

    enum class ActionType {
        NONE,
        GO,
        NEXT,
        PREVIOUS,
        SEARCH,
        SEND,
        DONE,
    }
}
