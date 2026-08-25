package com.bykunalbuilds.backgroundremover.ui

import android.graphics.Bitmap
import android.net.Uri

data class RemovalUiState(
    val phase: RemovalPhase = RemovalPhase.EMPTY,
    val original: Bitmap? = null,
    val result: Bitmap? = null,
    val isSaving: Boolean = false,
    val savedUri: Uri? = null,
) {
    val isProcessing: Boolean get() = phase == RemovalPhase.PROCESSING
    val hasResult: Boolean get() = original != null && result != null
}

enum class RemovalPhase { EMPTY, PROCESSING, RESULT }

sealed interface RemovalEvent {
    data object ImageSelected : RemovalEvent
    data object ProcessingSucceeded : RemovalEvent
    data object ProcessingFailed : RemovalEvent
    data object Reset : RemovalEvent
}

object RemovalStateReducer {
    fun reduce(phase: RemovalPhase, event: RemovalEvent): RemovalPhase = when (event) {
        RemovalEvent.ImageSelected -> RemovalPhase.PROCESSING
        RemovalEvent.ProcessingSucceeded -> if (phase == RemovalPhase.PROCESSING) RemovalPhase.RESULT else phase
        RemovalEvent.ProcessingFailed -> if (phase == RemovalPhase.PROCESSING) RemovalPhase.EMPTY else phase
        RemovalEvent.Reset -> RemovalPhase.EMPTY
    }
}

sealed interface UiEffect {
    data class ShowMessage(val message: String) : UiEffect
    data class ShareImage(val uri: Uri) : UiEffect
}
