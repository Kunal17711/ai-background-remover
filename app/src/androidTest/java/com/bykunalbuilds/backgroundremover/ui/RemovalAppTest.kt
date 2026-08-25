package com.bykunalbuilds.backgroundremover.ui

import android.graphics.Bitmap
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.bykunalbuilds.backgroundremover.ui.theme.BackgroundRemoverTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RemovalAppTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun initialCallToActionLaunchesPicker() {
        var clicked = false
        compose.setContent {
            BackgroundRemoverTheme {
                RemovalApp(
                    state = RemovalUiState(),
                    snackbarHostState = SnackbarHostState(),
                    onSelectImage = { clicked = true },
                    onSave = {},
                    onShare = {},
                    onChooseAnother = {},
                    onOpenInstagram = {},
                )
            }
        }

        compose.onNodeWithText("Select an image").assertIsDisplayed().performClick()
        assertTrue(clicked)
    }

    @Test
    fun resultShowsPrimaryExportActions() {
        val original = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        val result = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        compose.setContent {
            BackgroundRemoverTheme {
                RemovalApp(
                    state = RemovalUiState(
                        phase = RemovalPhase.RESULT,
                        original = original,
                        result = result,
                    ),
                    snackbarHostState = SnackbarHostState(),
                    onSelectImage = {},
                    onSave = {},
                    onShare = {},
                    onChooseAnother = {},
                    onOpenInstagram = {},
                )
            }
        }

        compose.onNodeWithText("Save PNG").assertIsDisplayed()
        compose.onNodeWithText("Share").assertIsDisplayed()
        compose.onNodeWithText("Choose another").assertIsDisplayed()
    }
}
