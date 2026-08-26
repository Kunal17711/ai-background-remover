package com.bykunalbuilds.backgroundremover.baselineprofile

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.provider.MediaStore
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun startup() = baselineProfileRule.collect(
        packageName = PACKAGE_NAME,
        includeInStartupProfile = true,
    ) {
        pressHome()
        startActivityAndWait()
        assertNotNull(device.wait(Until.findObject(By.text("Start with a photo")), UI_TIMEOUT_MS))
    }

    @Test
    fun selectImageAndStartProcessing() {
        val fixture = createFixtureImage()
        try {
            baselineProfileRule.collect(
                packageName = PACKAGE_NAME,
                includeInStartupProfile = false,
            ) {
                startActivityAndWait()
                val selectImage = device.wait(
                    Until.findObject(
                        By.clickable(true).hasDescendant(By.text("Select an image")),
                    ),
                    UI_TIMEOUT_MS,
                )
                assertNotNull(selectImage)
                selectImage.click()

                val pickerItem = device.wait(
                    Until.findObject(By.res(AOSP_PICKER_PACKAGE, PICKER_THUMBNAIL_ID)),
                    PICKER_RESOURCE_TIMEOUT_MS,
                ) ?: device.wait(
                    Until.findObject(By.res(GOOGLE_PICKER_PACKAGE, PICKER_THUMBNAIL_ID)),
                    PICKER_RESOURCE_TIMEOUT_MS,
                )
                    ?: device.wait(
                        Until.findObject(By.clickable(true).minDepth(4)),
                        PICKER_TIMEOUT_MS,
                    )
                assertNotNull(pickerItem)
                pickerItem.click()

                assertNotNull(
                    device.wait(Until.findObject(By.text("Removing background…")), UI_TIMEOUT_MS),
                )
                // Capture model copy/session initialization without making profile
                // generation depend on unaccelerated x86 inference completing.
                device.wait(Until.hasObject(By.text("Save PNG")), MODEL_WARMUP_TIMEOUT_MS)
            }
        } finally {
            deleteFixtureImage(fixture)
        }
    }

    private fun createFixtureImage(): Uri {
        val context = InstrumentationRegistry.getInstrumentation().context
        val bitmap = Bitmap.createBitmap(FIXTURE_SIZE, FIXTURE_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(225, 235, 245))
        canvas.drawCircle(
            FIXTURE_SIZE / 2f,
            FIXTURE_SIZE / 2f,
            FIXTURE_SIZE / 3f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(30, 40, 50) },
        )
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, FIXTURE_NAME)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AI Background Remover Profile")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = requireNotNull(resolver.insert(collection, values))
        try {
            resolver.openOutputStream(uri, "w")!!.use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
            }
            resolver.update(
                uri,
                ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
                null,
                null,
            )
            return uri
        } catch (error: Throwable) {
            resolver.delete(uri, null, null)
            throw error
        } finally {
            bitmap.recycle()
        }
    }

    private fun deleteFixtureImage(uri: Uri) {
        runCatching {
            InstrumentationRegistry.getInstrumentation().context.contentResolver.delete(uri, null, null)
        }
    }

    companion object {
        private const val PACKAGE_NAME = "com.bykunalbuilds.backgroundremover"
        private const val AOSP_PICKER_PACKAGE = "com.android.providers.media.module"
        private const val GOOGLE_PICKER_PACKAGE = "com.google.android.providers.media.module"
        private const val PICKER_THUMBNAIL_ID = "icon_thumbnail"
        private const val FIXTURE_NAME = "ai-background-remover-baseline-profile.png"
        private const val FIXTURE_SIZE = 512
        private const val UI_TIMEOUT_MS = 15_000L
        private const val PICKER_RESOURCE_TIMEOUT_MS = 5_000L
        private const val PICKER_TIMEOUT_MS = 30_000L
        private const val MODEL_WARMUP_TIMEOUT_MS = 30_000L
    }
}
