package org.fcitx.fcitx5.android.core

sealed class FcitxEvent<T>(open val data: T) {

    abstract val eventType: EventType

    data class CandidateListEvent(override val data: Data) :
        FcitxEvent<CandidateListEvent.Data>(data) {

        override val eventType: EventType
            get() = EventType.Candidate

        data class Data(val total: Int, val candidates: Array<String>) {

            override fun toString(): String =
                "total=$total, candidates=[${candidates.joinToString(limit = 5)}]"

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Data

                if (total != other.total) return false
                if (!candidates.contentEquals(other.candidates)) return false

                return true
            }

            override fun hashCode(): Int {
                var result = total
                result = 31 * result + candidates.contentHashCode()
                return result
            }
        }
    }

    data class CommitStringEvent(override val data: Data) :
        FcitxEvent<CommitStringEvent.Data>(data) {
        override val eventType: EventType
            get() = EventType.Commit

        data class Data(val text: String, val cursor: Int)
    }

    data class ClientPreeditEvent(override val data: FormattedText) :
        FcitxEvent<FormattedText>(data) {
        override val eventType: EventType
            get() = EventType.ClientPreedit

        override fun toString(): String = "ClientPreeditEvent('$data', ${data.cursor})"
    }

    data class InputPanelEvent(override val data: Data) : FcitxEvent<InputPanelEvent.Data>(data) {
        override val eventType: EventType
            get() = EventType.InputPanel

        data class Data(
            val preedit: FormattedText,
            val auxUp: FormattedText,
            val auxDown: FormattedText
        ) {
            constructor() : this(FormattedText.Empty, FormattedText.Empty, FormattedText.Empty)
        }
    }

    data class ReadyEvent(override val data: Unit = Unit) : FcitxEvent<Unit>(data) {
        override val eventType: EventType
            get() = EventType.Ready

        override fun toString(): String = "ReadyEvent"
    }

    data class KeyEvent(override val data: Data) : FcitxEvent<KeyEvent.Data>(data) {
        override val eventType: EventType
            get() = EventType.Key

        data class Data(
            val sym: KeySym,
            val states: KeyStates,
            val unicode: Int,
            val up: Boolean,
            val timestamp: Int
        )
    }

    data class IMChangeEvent(override val data: InputMethodEntry) :
        FcitxEvent<InputMethodEntry>(data) {
        override val eventType: EventType
            get() = EventType.Change
    }

    data class StatusAreaEvent(override val data: Data) : FcitxEvent<StatusAreaEvent.Data>(data) {
        override val eventType: EventType
            get() = EventType.StatusArea

        data class Data(val actions: Array<Action>, val im: InputMethodEntry) {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Data

                if (!actions.contentEquals(other.actions)) return false
                if (im != other.im) return false

                return true
            }

            override fun hashCode(): Int {
                var result = actions.contentHashCode()
                result = 31 * result + im.hashCode()
                return result
            }
        }
    }

    data class UnknownEvent(override val data: Array<Any>) : FcitxEvent<Array<Any>>(data) {
        override val eventType: EventType
            get() = EventType.Unknown

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as UnknownEvent

            if (!data.contentEquals(other.data)) return false

            return true
        }

        override fun hashCode(): Int {
            return data.contentHashCode()
        }
    }

    enum class EventType {
        Candidate,
        Commit,
        ClientPreedit,
        InputPanel,
        Ready,
        Key,
        Change,
        StatusArea,
        Unknown
    }

    companion object {

        private val Types = EventType.values()

        @Suppress("UNCHECKED_CAST")
        fun create(type: Int, params: Array<Any>) =
            when (Types[type]) {
                EventType.Candidate -> CandidateListEvent(
                    CandidateListEvent.Data(
                        params[0] as Int,
                        params[1] as Array<String>
                    )
                )
                EventType.Commit -> CommitStringEvent(
                    CommitStringEvent.Data(
                        params[0] as String,
                        params[1] as Int
                    )
                )
                EventType.ClientPreedit -> ClientPreeditEvent(params[0] as FormattedText)
                EventType.InputPanel -> InputPanelEvent(
                    InputPanelEvent.Data(
                        params[0] as FormattedText,
                        params[1] as FormattedText,
                        params[2] as FormattedText
                    )
                )
                EventType.Ready -> ReadyEvent()
                EventType.Key -> KeyEvent(
                    KeyEvent.Data(
                        KeySym(params[0] as Int),
                        KeyStates.of(params[1] as Int),
                        params[2] as Int,
                        params[3] as Boolean,
                        params[4] as Int
                    )
                )
                EventType.Change -> IMChangeEvent(params[0] as InputMethodEntry)
                EventType.StatusArea -> StatusAreaEvent(
                    StatusAreaEvent.Data(
                        params[0] as Array<Action>,
                        params[1] as InputMethodEntry
                    )
                )
                else -> UnknownEvent(params)
            }
    }
}
