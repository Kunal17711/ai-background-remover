package com.bykunalbuilds.backgroundremover.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class OutputNamesTest {
    @Test
    fun pngNameIsStableAndFilesystemSafe() {
        val clock = Clock.fixed(Instant.parse("2026-08-26T10:15:30Z"), ZoneOffset.UTC)
        assertEquals("AI_Background_Remover_20260826_101530.png", OutputNames.png(clock))
    }
}
