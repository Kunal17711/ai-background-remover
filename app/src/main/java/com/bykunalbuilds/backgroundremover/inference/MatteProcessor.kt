package com.bykunalbuilds.backgroundremover.inference

import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

object MatteProcessor {
    fun logitsToAlpha(logits: FloatArray): ByteArray {
        require(logits.isNotEmpty())
        return ByteArray(logits.size) { index ->
            val value = logits[index]
            require(value.isFinite()) { "Model output contains a non-finite value." }
            val probability = if (value >= 0f) {
                (1.0 / (1.0 + exp(-value.toDouble()))).toFloat()
            } else {
                val exponential = exp(value.toDouble())
                (exponential / (1.0 + exponential)).toFloat()
            }
            val refined = when {
                probability < 0.008f -> 0f
                probability > 0.995f -> 1f
                else -> probability
            }
            (min(255, max(0, (refined * 255f + 0.5f).toInt()))).toByte()
        }
    }
}
