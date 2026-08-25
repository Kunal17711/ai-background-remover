package com.bykunalbuilds.backgroundremover.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

class ImageDecoder(private val context: Context) {
    suspend fun decode(uri: Uri): Bitmap = withContext(Dispatchers.IO) {
        try {
            coroutineContext.ensureActive()
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                decodeModern(uri)
            } else {
                decodeLegacy(uri)
            }
            coroutineContext.ensureActive()
            require(bitmap.width > 0 && bitmap.height > 0)
            bitmap
        } catch (error: OutOfMemoryError) {
            throw ImageDecodeException("This photo is too large for the available memory.", error)
        } catch (error: SecurityException) {
            throw ImageDecodeException("The selected photo is no longer available.", error)
        } catch (error: ImageDecodeException) {
            throw error
        } catch (error: Exception) {
            throw ImageDecodeException("This image could not be opened. Try another JPEG, PNG, or WebP file.", error)
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun decodeModern(uri: Uri): Bitmap {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val plan = DecodePlan.calculate(info.size.width, info.size.height)
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.memorySizePolicy = ImageDecoder.MEMORY_POLICY_LOW_RAM
            decoder.isMutableRequired = false
            if (plan.sampleSize > 1) decoder.setTargetSampleSize(plan.sampleSize)
        }
    }

    private fun decodeLegacy(uri: Uri): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        } ?: throw ImageDecodeException("The selected photo is no longer available.")
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw ImageDecodeException("This image format is not supported.")
        }

        val plan = DecodePlan.calculate(bounds.outWidth, bounds.outHeight)
        var powerOfTwoSample = 1
        while (powerOfTwoSample < plan.sampleSize) powerOfTwoSample *= 2
        val options = BitmapFactory.Options().apply {
            inSampleSize = powerOfTwoSample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: throw ImageDecodeException("The selected photo could not be decoded.")

        val orientation = context.contentResolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        } ?: ExifInterface.ORIENTATION_NORMAL
        return applyOrientation(decoded, orientation)
    }

    private fun applyOrientation(source: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
            else -> return source
        }
        val corrected = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        if (corrected !== source) source.recycle()
        return corrected
    }
}

class ImageDecodeException(message: String, cause: Throwable? = null) : Exception(message, cause)
