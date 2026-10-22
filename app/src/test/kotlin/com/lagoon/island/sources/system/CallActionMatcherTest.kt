package com.lagoon.island.sources.system

import com.lagoon.island.engine.ActivityContent.Call.State
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CallActionMatcherTest {
    private fun candidates(vararg labels: String?) = labels.mapIndexed { i, l -> CallActionCandidate(i, l) }

    @Test
    fun `google dialer incoming call`() {
        val m = CallActionMatcher.match(candidates("Decline", "Answer"))
        assertEquals(CallActions(answer = 1, decline = 0, hangUp = null), m)
    }

    @Test
    fun `ongoing call with hang up`() {
        val m = CallActionMatcher.match(candidates("Hang up", "Speaker"))
        assertEquals(CallActions(hangUp = 0), m)
    }

    @Test
    fun `samsung style accept reject and end`() {
        assertEquals(CallActions(answer = 1, decline = 0), CallActionMatcher.match(candidates("Reject", "Accept")))
        assertEquals(CallActions(hangUp = 0), CallActionMatcher.match(candidates("End", "Mute")))
    }

    @Test
    fun `ambiguous end means decline when an answer button exists`() {
        val m = CallActionMatcher.match(candidates("Answer", "End"))
        assertEquals(CallActions(answer = 0, decline = 1), m)
    }

    @Test
    fun `icons fill in when labels say nothing`() {
        val m = CallActionMatcher.match(
            listOf(
                CallActionCandidate(0, "", "ic_call_answer"),
                CallActionCandidate(1, null, "ic_call_end"),
            ),
        )
        assertEquals(CallActions(answer = 0, decline = 1), m)
        val ongoing = CallActionMatcher.match(listOf(CallActionCandidate(0, null, "ic_call_end")))
        assertEquals(CallActions(hangUp = 0), ongoing)
    }

    @Test
    fun `localised labels`() {
        assertEquals(CallActions(answer = 0, decline = 1), CallActionMatcher.match(candidates("Annehmen", "Ablehnen")))
        assertEquals(CallActions(answer = 1, decline = 0), CallActionMatcher.match(candidates("Refuser", "Répondre")))
        assertEquals(CallActions(hangUp = 0), CallActionMatcher.match(candidates("Auflegen")))
        assertEquals(CallActions(answer = 1, decline = 0), CallActionMatcher.match(candidates("Сбросить", "Принять")))
        assertEquals(CallActions(answer = 0, decline = 1), CallActionMatcher.match(candidates("Ответить", "Отклонить")))
        assertEquals(CallActions(hangUp = 0), CallActionMatcher.match(candidates("Завершить", "Динамик")))
        assertEquals(CallActions(answer = 0, decline = 1), CallActionMatcher.match(candidates("Aceptar", "Rechazar")))
        assertEquals(CallActions(hangUp = 0), CallActionMatcher.match(candidates("Terminar")))
        assertEquals(CallActions(answer = 1, decline = 0), CallActionMatcher.match(candidates("Ignorer", "Accepter")))
        assertEquals(CallActions(answer = 1, decline = 0), CallActionMatcher.match(candidates("Ignorieren", "Abheben")))
    }

    @Test
    fun `word boundaries keep send and friend out`() {
        assertNull(CallActionMatcher.classify("Send message"))
        assertNull(CallActionMatcher.classify("Friend"))
        assertEquals(CallActionKind.HangUp, CallActionMatcher.classify("End"))
        assertEquals(CallActionKind.Decline, CallActionMatcher.classify("End", hasAnswer = true))
        assertEquals(CallActionKind.HangUp, CallActionMatcher.classify("End call", hasAnswer = true))
    }

    @Test
    fun `state inference prefers call style, then telephony, then buttons`() {
        assertEquals(State.Ringing, CallStateInference.infer(CallStateInference.CALL_TYPE_INCOMING, CallStateInference.TELEPHONY_OFFHOOK, false))
        assertEquals(State.Active, CallStateInference.infer(CallStateInference.CALL_TYPE_ONGOING, CallStateInference.TELEPHONY_RINGING, true))
        assertEquals(State.Ringing, CallStateInference.infer(CallStateInference.CALL_TYPE_SCREENING, null, false))
        assertEquals(State.Ringing, CallStateInference.infer(null, CallStateInference.TELEPHONY_RINGING, false))
        assertEquals(State.Active, CallStateInference.infer(null, CallStateInference.TELEPHONY_OFFHOOK, true))
        assertEquals(State.Ringing, CallStateInference.infer(null, null, true))
        assertEquals(State.Active, CallStateInference.infer(null, null, false))
        assertEquals(State.Active, CallStateInference.infer(CallStateInference.CALL_TYPE_UNKNOWN, CallStateInference.TELEPHONY_IDLE, false))
    }
}
