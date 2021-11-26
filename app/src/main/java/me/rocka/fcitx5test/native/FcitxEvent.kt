package me.rocka.fcitx5test.native

sealed class FcitxEvent<T>(open val data: T) {

    abstract val eventType: EventType

    data class CandidateListEvent(override val data: List<String>) :
        FcitxEvent<List<String>>(data) {
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

    data class UnknownEvent(override val data: List<Any>) : FcitxEvent<List<Any>>(data) {
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

        @Suppress("UNCHECKED_CAST")
        fun create(type: Int, params: List<Any>) =
            when (EventType.values()[type]) {
                EventType.Candidate -> CandidateListEvent(params as List<String>)
                EventType.Commit -> CommitStringEvent(params.first() as String)
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