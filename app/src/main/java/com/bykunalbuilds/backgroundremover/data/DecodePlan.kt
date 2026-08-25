package com.bykunalbuilds.backgroundremover.data

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.sqrt

data class DecodePlan(
    val sampleSize: Int,
    val outputWidth: Int,
    val outputHeight: Int,
) {
    companion object {
        const val DEFAULT_MAX_PIXELS = 12_000_000L
        const val DEFAULT_MAX_DIMENSION = 6_144

        fun calculate(
            width: Int,
            height: Int,
            maxPixels: Long = DEFAULT_MAX_PIXELS,
            maxDimension: Int = DEFAULT_MAX_DIMENSION,
        ): DecodePlan {
            require(width > 0 && height > 0)
            require(maxPixels > 0 && maxDimension > 0)

            val pixelRatio = sqrt((width.toDouble() * height) / maxPixels.toDouble())
            val edgeRatio = max(
                width.toDouble() / maxDimension,
                height.toDouble() / maxDimension,
            )
            val sample = max(1, ceil(max(pixelRatio, edgeRatio)).toInt())
            return DecodePlan(
                sampleSize = sample,
                outputWidth = max(1, width / sample),
                outputHeight = max(1, height / sample),
            )
        }
    }
}
