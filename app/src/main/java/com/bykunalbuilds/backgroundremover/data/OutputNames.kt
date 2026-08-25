package com.bykunalbuilds.backgroundremover.data

import java.time.Clock
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object OutputNames {
    private val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")

    fun png(clock: Clock = Clock.systemDefaultZone()): String =
        "AI_Background_Remover_${LocalDateTime.now(clock).format(formatter)}.png"
}
