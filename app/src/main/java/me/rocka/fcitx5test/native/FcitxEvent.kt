package me.rocka.fcitx5test.native

sealed class FcitxEvent<T>(open val data: T) {
    data class CandidateListEvent(override val data: List<String>) :
        FcitxEvent<List<String>>(data)

    data class CommitStringEvent(override val data: String) :
        FcitxEvent<String>(data)

    data class PreeditEventData(val preedit: String, val clientPreedit: String)

    data class PreeditEvent(override val data: PreeditEventData) :
        FcitxEvent<PreeditEventData>(data)

    data class UnknownEvent(override val data: List<Any>) : FcitxEvent<List<Any>>(data)

    companion object {
        private const val CANDIDATE_LIST_ID = 0
        private const val COMMIT_STRING_ID = 1
        private const val PREEDIT_ID = 2

        @Suppress("UNCHECKED_CAST")
        fun create(type: Int, params: List<Any>) =
            when (type) {
                CANDIDATE_LIST_ID -> CandidateListEvent(params as List<String>)
                COMMIT_STRING_ID -> CommitStringEvent(params.first() as String)
                PREEDIT_ID -> PreeditEvent(PreeditEventData(params[0] as String, params[1] as String))
                else -> UnknownEvent(params)
            }
    }
}