package com.bykunalbuilds.backgroundremover.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ImageExporter(private val context: Context) {
    suspend fun saveToPictures(bitmap: Bitmap): Uri = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, OutputNames.png())
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AI Background Remover")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val uri = resolver.insert(collection, values)
            ?: throw ImageExportException("Android could not create the output file.")
        try {
            resolver.openOutputStream(uri, "w")?.use { stream ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                    throw ImageExportException("The transparent PNG could not be encoded.")
                }
            } ?: throw ImageExportException("The output file could not be opened.")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                resolver.update(uri, ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }, null, null)
            }
            uri
        } catch (error: Exception) {
            resolver.delete(uri, null, null)
            if (error is ImageExportException) throw error
            throw ImageExportException("The PNG could not be saved. Check available storage and try again.", error)
        }
    }

    suspend fun createShareUri(bitmap: Bitmap): Uri = withContext(Dispatchers.IO) {
        try {
            val directory = File(context.cacheDir, "shared").apply { mkdirs() }
            directory.listFiles()?.filter { it.isFile }?.forEach { old ->
                if (System.currentTimeMillis() - old.lastModified() > SHARE_CACHE_MAX_AGE_MS) old.delete()
            }
            val file = File(directory, OutputNames.png())
            FileOutputStream(file).use { stream ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                    throw ImageExportException("The transparent PNG could not be prepared for sharing.")
                }
            }
            FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        } catch (error: Exception) {
            if (error is ImageExportException) throw error
            throw ImageExportException("The PNG could not be shared. Try saving it first.", error)
        }
    }

    companion object {
        private const val SHARE_CACHE_MAX_AGE_MS = 24L * 60L * 60L * 1_000L
    }
}

class ImageExportException(message: String, cause: Throwable? = null) : Exception(message, cause)
