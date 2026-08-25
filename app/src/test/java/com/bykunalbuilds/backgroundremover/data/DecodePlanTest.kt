package com.bykunalbuilds.backgroundremover.data

import org.junit.Assert.assertEquals
import org.junit.Test

class DecodePlanTest {
    @Test
    fun smallImageIsNotDownsampled() {
        val plan = DecodePlan.calculate(1_920, 1_080)
        assertEquals(1, plan.sampleSize)
        assertEquals(1_920, plan.outputWidth)
        assertEquals(1_080, plan.outputHeight)
    }

    @Test
    fun veryLargePortraitIsBoundedByPixelBudget() {
        val plan = DecodePlan.calculate(8_000, 12_000)
        assertEquals(3, plan.sampleSize)
        check(plan.outputWidth.toLong() * plan.outputHeight <= DecodePlan.DEFAULT_MAX_PIXELS)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidMetadataIsRejected() {
        DecodePlan.calculate(0, 100)
    }
}
