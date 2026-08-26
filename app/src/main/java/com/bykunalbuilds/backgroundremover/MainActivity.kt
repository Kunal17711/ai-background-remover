package com.bykunalbuilds.backgroundremover

import android.content.ClipData
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bykunalbuilds.backgroundremover.ui.RemovalApp
import com.bykunalbuilds.backgroundremover.ui.RemovalViewModel
import com.bykunalbuilds.backgroundremover.ui.UiEffect
import com.bykunalbuilds.backgroundremover.ui.theme.BackgroundRemoverTheme

class MainActivity : ComponentActivity() {
    private val viewModel: RemovalViewModel by viewModels {
        RemovalViewModel.Factory((application as BackgroundRemoverApplication).container)
    }

    private val photoPicker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(viewModel::selectImage)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            BackgroundRemoverTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                val snackbarHostState = remember { SnackbarHostState() }
                LaunchedEffect(viewModel) {
                    viewModel.effects.collect { effect ->
                        when (effect) {
                            is UiEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
                            is UiEffect.ShareImage -> shareImage(effect.uri, snackbarHostState)
                        }
                    }
                }
                RemovalApp(
                    state = state,
                    snackbarHostState = snackbarHostState,
                    onSelectImage = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onSave = viewModel::save,
                    onShare = viewModel::share,
                    onChooseAnother = viewModel::chooseAnother,
                    onOpenInstagram = ::openInstagram,
                )
            }
        }
    }

    private suspend fun shareImage(uri: Uri, snackbar: SnackbarHostState) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newUri(contentResolver, "Transparent PNG", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(Intent.createChooser(intent, "Share transparent PNG"))
        } catch (_: Exception) {
            snackbar.showSnackbar("No app is available to share this PNG.")
        }
    }

    private fun openInstagram() {
        val intent = Intent(Intent.ACTION_VIEW, INSTAGRAM_URL.toUri())
        try {
            startActivity(intent)
        } catch (_: Exception) {
            // A device without a browser cannot open the creator link.
        }
    }

    companion object {
        private const val INSTAGRAM_URL = "https://instagram.com/bykunalbuilds"
    }
}
