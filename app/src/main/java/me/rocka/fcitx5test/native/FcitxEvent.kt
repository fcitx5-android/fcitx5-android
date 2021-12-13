package me.rocka.fcitx5test.native

sealed class FcitxEvent<T>(open val data: T) {

    abstract val eventType: EventType

    data class CandidateListEvent(override val data: Array<String>) :
        FcitxEvent<Array<String>>(data) {
        override val eventType: EventType
            get() = EventType.Candidate
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

        data class Data(val preedit: String, val clientPreedit: String, val cursor: Int)
    }

    data class InputPanelAuxEvent(override val data: Data) :
        FcitxEvent<InputPanelAuxEvent.Data>(data) {
        override val eventType: EventType
            get() = EventType.Aux

        data class Data(val auxUp: String, val auxDown: String)
    }

    data class ReadyEvent(override val data: Unit = Unit) : FcitxEvent<Unit>(data) {
        override val eventType: EventType
            get() = EventType.Ready
    }

    data class KeyEvent(override val data: Data) :
        FcitxEvent<KeyEvent.Data>(data) {
        override val eventType: EventType
            get() = EventType.Key

        data class Data(val code: Int, val sym: String)
    }

    data class IMChangeEvent(override val data: Data) :
        FcitxEvent<IMChangeEvent.Data>(data) {
        override val eventType: EventType
            get() = EventType.Change

        data class Data(val status: InputMethodEntry)
    }

    data class UnknownEvent(override val data: Array<Any>) : FcitxEvent<Array<Any>>(data) {
        override val eventType: EventType
            get() = EventType.Unknown
    }

    enum class EventType {
        Candidate,
        Commit,
        Preedit,
        Aux,
        Ready,
        Key,
        Change,
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
                    PreeditEvent.Data(params[0] as String, params[1] as String, params[2] as Int)
                )
                EventType.Aux -> InputPanelAuxEvent(
                    InputPanelAuxEvent.Data(params[0] as String, params[1] as String)
                )
                EventType.Ready -> ReadyEvent()
                EventType.Key -> KeyEvent(KeyEvent.Data(params[0] as Int, params[1] as String))
                EventType.Change -> IMChangeEvent(IMChangeEvent.Data(params[0] as InputMethodEntry))
                else -> UnknownEvent(params)
            }
    }
}