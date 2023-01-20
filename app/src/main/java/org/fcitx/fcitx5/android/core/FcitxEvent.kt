package org.fcitx.fcitx5.android.core

sealed class FcitxEvent<T>(open val data: T) {

    abstract val eventType: EventType

    data class CandidateListEvent(override val data: Array<String>) :
        FcitxEvent<Array<String>>(data) {
        override val eventType: EventType
            get() = EventType.Candidate

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as CandidateListEvent

            if (!data.contentEquals(other.data)) return false

            return true
        }

        override fun hashCode(): Int {
            return data.contentHashCode()
        }

        override fun toString(): String = "CandidateListEvent(data=[${
            data.take(5).joinToString()
        }${if (data.size > 5) ", ..." else ""}])"
    }

    data class CommitStringEvent(override val data: String) :
        FcitxEvent<String>(data) {
        override val eventType: EventType
            get() = EventType.Commit
    }

    data class PreeditEvent(override val data: Data) :
        FcitxEvent<PreeditEvent.Data>(data) {
        override val eventType: EventType
            get() = EventType.Preedit

        data class Data(val preedit: FormattedText, val clientPreedit: FormattedText) {
            constructor() : this(FormattedText(), FormattedText())
        }

        override fun toString(): String {
            return "PreeditEvent(preedit=[${data.preedit}, ${data.preedit.cursor}], clientPreedit=[${data.clientPreedit}, ${data.clientPreedit.cursor}])"
        }
    }

    data class InputPanelAuxEvent(override val data: Data) :
        FcitxEvent<InputPanelAuxEvent.Data>(data) {
        override val eventType: EventType
            get() = EventType.Aux

        data class Data(val auxUp: FormattedText, val auxDown: FormattedText) {
            constructor() : this(FormattedText(), FormattedText())
        }
    }

    data class ReadyEvent(override val data: Unit = Unit) : FcitxEvent<Unit>(data) {
        override val eventType: EventType
            get() = EventType.Ready

        override fun toString(): String = "ReadyEvent"
    }

    data class KeyEvent(override val data: Data) :
        FcitxEvent<KeyEvent.Data>(data) {
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

    data class StatusAreaEvent(override val data: Array<Action>) :
        FcitxEvent<Array<Action>>(data) {

        override val eventType: EventType
            get() = EventType.StatusArea

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as StatusAreaEvent

            if (!data.contentEquals(other.data)) return false

            return true
        }

        override fun hashCode(): Int {
            return data.contentHashCode()
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
        Preedit,
        Aux,
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
                EventType.Candidate -> CandidateListEvent(params as Array<String>)
                EventType.Commit -> CommitStringEvent(params[0] as String)
                EventType.Preedit -> PreeditEvent(
                    PreeditEvent.Data(params[0] as FormattedText, params[1] as FormattedText)
                )
                EventType.Aux -> InputPanelAuxEvent(
                    InputPanelAuxEvent.Data(params[0] as FormattedText, params[1] as FormattedText)
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
                EventType.StatusArea -> StatusAreaEvent(params as Array<Action>)
                else -> UnknownEvent(params)
            }
    }
}
