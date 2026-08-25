package com.bykunalbuilds.backgroundremover.inference

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MatteProcessorTest {
    @Test
    fun logitsBecomeMonotonicAlphaValues() {
        val alpha = MatteProcessor.logitsToAlpha(floatArrayOf(-20f, 0f, 20f))
        assertEquals(0, alpha[0].toInt() and 0xFF)
        assertTrue((alpha[1].toInt() and 0xFF) in 127..128)
        assertEquals(255, alpha[2].toInt() and 0xFF)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidModelOutputIsRejected() {
        MatteProcessor.logitsToAlpha(floatArrayOf(Float.NaN))
    }
}
