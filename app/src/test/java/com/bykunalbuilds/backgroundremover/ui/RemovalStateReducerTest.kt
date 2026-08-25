package com.bykunalbuilds.backgroundremover.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class RemovalStateReducerTest {
    @Test
    fun happyPathMovesFromEmptyToProcessingToResult() {
        var phase = RemovalPhase.EMPTY
        phase = RemovalStateReducer.reduce(phase, RemovalEvent.ImageSelected)
        assertEquals(RemovalPhase.PROCESSING, phase)
        phase = RemovalStateReducer.reduce(phase, RemovalEvent.ProcessingSucceeded)
        assertEquals(RemovalPhase.RESULT, phase)
    }

    @Test
    fun failureReturnsProcessingToEmpty() {
        val phase = RemovalStateReducer.reduce(RemovalPhase.PROCESSING, RemovalEvent.ProcessingFailed)
        assertEquals(RemovalPhase.EMPTY, phase)
    }

    @Test
    fun staleSuccessCannotReplaceResetState() {
        val reset = RemovalStateReducer.reduce(RemovalPhase.PROCESSING, RemovalEvent.Reset)
        val staleSuccess = RemovalStateReducer.reduce(reset, RemovalEvent.ProcessingSucceeded)
        assertEquals(RemovalPhase.EMPTY, staleSuccess)
    }
}
