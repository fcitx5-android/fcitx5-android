package me.rocka.fcitx5test.native

sealed class FcitxEvent<T>(open val data: T) {
    data class CandidateListEvent(override val data: List<String>) :
        FcitxEvent<List<String>>(data)

    data class CommitStringEvent(override val data: String) :
        FcitxEvent<String>(data)

    data class PreeditEvent(override val data: Data) :
        FcitxEvent<PreeditEvent.Data>(data) {
        data class Data(val preedit: String, val clientPreedit: String)
    }

    data class InputPanelAuxEvent(override val data: Data) :
        FcitxEvent<InputPanelAuxEvent.Data>(data) {
        data class Data(val auxUp: String, val auxDown: String)
    }

    data class KeyEvent(override val data: Data) :
        FcitxEvent<KeyEvent.Data>(data) {
        data class Data(val code: Int, val sym: String)
    }

    data class ReadyEvent(override val data: Unit = Unit) : FcitxEvent<Unit>(data)

    data class UnknownEvent(override val data: List<Any>) : FcitxEvent<List<Any>>(data)

    companion object {
        private const val CANDIDATE_LIST_ID = 0
        private const val COMMIT_STRING_ID = 1
        private const val PREEDIT_ID = 2
        private const val INPUT_PANEL_AUX_ID = 3
        private const val READY_ID = 4;
        private const val KEY_EVENT_ID = 5;

        @Suppress("UNCHECKED_CAST")
        fun create(type: Int, params: List<Any>) =
            when (type) {
                CANDIDATE_LIST_ID -> CandidateListEvent(params as List<String>)
                COMMIT_STRING_ID -> CommitStringEvent(params.first() as String)
                PREEDIT_ID -> PreeditEvent(PreeditEvent.Data(params[0] as String, params[1] as String))
                INPUT_PANEL_AUX_ID -> InputPanelAuxEvent(InputPanelAuxEvent.Data(params[0] as String, params[1] as String))
                READY_ID -> ReadyEvent()
                KEY_EVENT_ID -> KeyEvent(KeyEvent.Data(params[0] as Int, params[1] as String))
                else -> UnknownEvent(params)
            }
    }
}