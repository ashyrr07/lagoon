package com.lagoon.island.sources.system

import com.lagoon.island.engine.ActivityContent

enum class CallActionKind { Answer, Decline, HangUp }

/** One of a notification's buttons, reduced to what the matcher needs. */
data class CallActionCandidate(val index: Int, val label: String?, val iconName: String? = null)

/** Indices (into the candidate list) of the three call controls, where found. */
data class CallActions(val answer: Int? = null, val decline: Int? = null, val hangUp: Int? = null) {
    operator fun get(kind: CallActionKind): Int? = when (kind) {
        CallActionKind.Answer -> answer
        CallActionKind.Decline -> decline
        CallActionKind.HangUp -> hangUp
    }
}

/**
 * Finds answer / decline / hang-up among a dialer notification's buttons when it does not use
 * CallStyle. Labels are matched by keyword in a handful of languages; icons by resource name.
 * "End" and a call_end icon are ambiguous: with an answer button present they mean decline,
 * otherwise hang up.
 */
object CallActionMatcher {
    private enum class Hint { Answer, Decline, HangUp, End }

    private val ANSWER = listOf(
        "answer", "accept", "pick up", "pickup", "annehmen", "répondre", "repondre", "rispondi", "responder",
        "contestar", "atender", "beantwoorden", "svara", "odbierz", "ответить", "応答", "接听", "접수", "수락",
    )
    private val DECLINE = listOf(
        "decline", "reject", "dismiss", "ignore", "ablehnen", "refuser", "rifiuta", "rechazar", "recusar",
        "rejeitar", "weigeren", "avvisa", "odrzuć", "odrzuc", "отклонить", "拒否", "拒绝", "거절",
    )
    private val HANG_UP = listOf(
        "hang up", "hangup", "end call", "disconnect", "auflegen", "raccrocher", "termina", "colgar",
        "desligar", "encerrar", "ophangen", "lägg på", "rozłącz", "rozlacz", "завершить", "終了", "挂断", "통화 종료",
    )
    private val END = listOf("end", "beenden", "fin", "terminer", "finalizar")

    private val ICON_ANSWER = listOf("answer", "accept", "call_received", "pickup")
    private val ICON_DECLINE = listOf("decline", "reject", "dismiss")
    private val ICON_HANG_UP = listOf("hangup", "hang_up", "disconnect")
    private val ICON_END = listOf("call_end", "end_call", "endcall")

    fun match(candidates: List<CallActionCandidate>): CallActions {
        val hints = candidates.map { it.index to (hintFor(it.label) ?: iconHintFor(it.iconName)) }
        val answer = hints.firstOrNull { it.second == Hint.Answer }?.first
        var decline = hints.firstOrNull { it.second == Hint.Decline }?.first
        var hangUp = hints.firstOrNull { it.second == Hint.HangUp }?.first
        val ends = hints.filter { it.second == Hint.End }.map { it.first }
        for (end in ends) {
            if (answer != null && decline == null) decline = end else if (hangUp == null) hangUp = end
        }
        return CallActions(answer, decline, hangUp)
    }

    fun classify(label: String?, iconName: String? = null, hasAnswer: Boolean = false): CallActionKind? =
        when (hintFor(label) ?: iconHintFor(iconName)) {
            Hint.Answer -> CallActionKind.Answer
            Hint.Decline -> CallActionKind.Decline
            Hint.HangUp -> CallActionKind.HangUp
            Hint.End -> if (hasAnswer) CallActionKind.Decline else CallActionKind.HangUp
            null -> null
        }

    private fun hintFor(label: String?): Hint? {
        val l = label?.trim()?.lowercase().orEmpty()
        if (l.isEmpty()) return null
        return when {
            HANG_UP.any { l.containsWord(it) } -> Hint.HangUp
            DECLINE.any { l.containsWord(it) } -> Hint.Decline
            ANSWER.any { l.containsWord(it) } -> Hint.Answer
            END.any { l.containsWord(it) } -> Hint.End
            else -> null
        }
    }

    private fun iconHintFor(iconName: String?): Hint? {
        val n = iconName?.lowercase().orEmpty()
        if (n.isEmpty()) return null
        return when {
            ICON_HANG_UP.any { it in n } -> Hint.HangUp
            ICON_DECLINE.any { it in n } -> Hint.Decline
            ICON_ANSWER.any { it in n } -> Hint.Answer
            ICON_END.any { it in n } -> Hint.End
            else -> null
        }
    }

    /** Substring match that does not fire inside a longer word ("end" must not match "send"). */
    private fun String.containsWord(word: String): Boolean {
        var from = 0
        while (true) {
            val at = indexOf(word, from)
            if (at < 0) return false
            val before = if (at == 0) ' ' else this[at - 1]
            val afterIndex = at + word.length
            val after = if (afterIndex >= length) ' ' else this[afterIndex]
            if (!before.isLetter() && !after.isLetter()) return true
            from = at + 1
        }
    }
}

/** Telephony and notification evidence combined into the island's three call states. */
object CallStateInference {
    const val TELEPHONY_IDLE = 0
    const val TELEPHONY_RINGING = 1
    const val TELEPHONY_OFFHOOK = 2

    const val CALL_TYPE_UNKNOWN = 0
    const val CALL_TYPE_INCOMING = 1
    const val CALL_TYPE_ONGOING = 2
    const val CALL_TYPE_SCREENING = 3

    fun infer(callStyleType: Int?, telephonyState: Int?, hasAnswerAction: Boolean): ActivityContent.Call.State =
        when (callStyleType) {
            CALL_TYPE_INCOMING, CALL_TYPE_SCREENING -> ActivityContent.Call.State.Ringing
            CALL_TYPE_ONGOING -> ActivityContent.Call.State.Active
            else -> when (telephonyState) {
                TELEPHONY_RINGING -> ActivityContent.Call.State.Ringing
                TELEPHONY_OFFHOOK -> ActivityContent.Call.State.Active
                else -> if (hasAnswerAction) ActivityContent.Call.State.Ringing else ActivityContent.Call.State.Active
            }
        }
}
