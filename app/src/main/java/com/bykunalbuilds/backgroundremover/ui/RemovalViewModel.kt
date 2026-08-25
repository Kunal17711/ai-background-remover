package com.bykunalbuilds.backgroundremover.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bykunalbuilds.backgroundremover.AppContainer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

class RemovalViewModel(private val container: AppContainer) : ViewModel() {
    private val generation = AtomicLong(0)
    private var processingJob: Job? = null
    private var actionJob: Job? = null

    private val _state = MutableStateFlow(RemovalUiState())
    val state: StateFlow<RemovalUiState> = _state.asStateFlow()

    private val _effects = Channel<UiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun selectImage(uri: Uri) {
        val request = generation.incrementAndGet()
        processingJob?.cancel()
        actionJob?.cancel()
        _state.value = RemovalUiState(
            phase = RemovalStateReducer.reduce(_state.value.phase, RemovalEvent.ImageSelected),
        )
        processingJob = viewModelScope.launch {
            try {
                val original = container.imageDecoder.decode(uri)
                if (request != generation.get()) {
                    original.recycle()
                    return@launch
                }
                _state.value = RemovalUiState(phase = RemovalPhase.PROCESSING, original = original)
                val result = container.backgroundRemover.removeBackground(original)
                if (request != generation.get()) {
                    result.recycle()
                    return@launch
                }
                _state.value = RemovalUiState(
                    phase = RemovalStateReducer.reduce(_state.value.phase, RemovalEvent.ProcessingSucceeded),
                    original = original,
                    result = result,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                if (request == generation.get()) {
                    val original = _state.value.original
                    _state.value = RemovalUiState(
                        phase = RemovalStateReducer.reduce(_state.value.phase, RemovalEvent.ProcessingFailed),
                        original = original,
                    )
                    _effects.send(UiEffect.ShowMessage(error.message ?: "Something went wrong. Please try again."))
                }
            }
        }
    }

    fun chooseAnother() {
        generation.incrementAndGet()
        processingJob?.cancel()
        actionJob?.cancel()
        _state.value = RemovalUiState(
            phase = RemovalStateReducer.reduce(_state.value.phase, RemovalEvent.Reset),
        )
    }

    fun save() {
        val result = _state.value.result ?: return
        if (_state.value.isSaving) return
        actionJob = viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)
            try {
                val uri = container.imageExporter.saveToPictures(result)
                _state.value = _state.value.copy(isSaving = false, savedUri = uri)
                _effects.send(UiEffect.ShowMessage("Transparent PNG saved to Pictures."))
            } catch (error: Throwable) {
                _state.value = _state.value.copy(isSaving = false)
                _effects.send(UiEffect.ShowMessage(error.message ?: "The PNG could not be saved."))
            }
        }
    }

    fun share() {
        val result = _state.value.result ?: return
        if (_state.value.isSaving) return
        actionJob = viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)
            try {
                val uri = container.imageExporter.createShareUri(result)
                _state.value = _state.value.copy(isSaving = false)
                _effects.send(UiEffect.ShareImage(uri))
            } catch (error: Throwable) {
                _state.value = _state.value.copy(isSaving = false)
                _effects.send(UiEffect.ShowMessage(error.message ?: "The PNG could not be shared."))
            }
        }
    }

    override fun onCleared() {
        generation.incrementAndGet()
        super.onCleared()
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(RemovalViewModel::class.java))
            return RemovalViewModel(container) as T
        }
    }
}
