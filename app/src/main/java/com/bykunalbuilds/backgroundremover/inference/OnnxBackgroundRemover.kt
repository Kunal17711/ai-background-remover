package com.bykunalbuilds.backgroundremover.inference

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.FloatBuffer
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext
import kotlin.math.min

class OnnxBackgroundRemover(private val context: Context) {
    private val mutex = Mutex()
    private val environment: OrtEnvironment by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        OrtEnvironment.getEnvironment()
    }
    private val session: OrtSession by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        createSession()
    }

    suspend fun removeBackground(source: Bitmap): Bitmap = mutex.withLock {
        withContext(Dispatchers.Default) {
            try {
                coroutineContext.ensureActive()
                val input = preprocess(source)
                coroutineContext.ensureActive()
                val matte = runInference(input)
                coroutineContext.ensureActive()
                composite(source, matte)
            } catch (error: OutOfMemoryError) {
                throw InferenceException("Not enough memory to process this photo. Try a smaller image.", error)
            } catch (error: InferenceException) {
                throw error
            } catch (error: Exception) {
                throw InferenceException("Background removal failed. Please try another image.", error)
            }
        }
    }

    private fun createSession(): OrtSession {
        try {
            val modelFile = prepareModelFile()
            OrtSession.SessionOptions().use { options ->
                options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.BASIC_OPT)
                options.setIntraOpNumThreads(min(4, Runtime.getRuntime().availableProcessors().coerceAtLeast(2)))
                options.setInterOpNumThreads(1)
                options.addConfigEntry("session.disable_prepacking", "1")
                return environment.createSession(modelFile.absolutePath, options)
            }
        } catch (error: OutOfMemoryError) {
            Log.e(TAG, "Not enough memory to initialize the ONNX model", error)
            throw InferenceException("Not enough memory to start the on-device AI model. Close other apps and try again.", error)
        } catch (error: LinkageError) {
            Log.e(TAG, "ONNX Runtime native library could not be loaded", error)
            throw InferenceException("The on-device AI runtime is not compatible with this device.", error)
        } catch (error: IllegalStateException) {
            Log.e(TAG, "The verified model file could not be prepared", error)
            throw InferenceException(error.message ?: "The on-device AI model could not be prepared.", error)
        } catch (error: Exception) {
            Log.e(TAG, "ONNX model session creation failed", error)
            throw InferenceException("The on-device AI model could not be loaded.", error)
        }
    }

    private fun prepareModelFile(): File {
        val modelDirectory = File(context.noBackupFilesDir, "models")
        if (!modelDirectory.exists() && !modelDirectory.mkdirs()) {
            throw IllegalStateException("The private model directory could not be created.")
        }

        val modelFile = File(modelDirectory, MODEL_FILE_NAME)
        val verificationFile = File(modelDirectory, "$MODEL_FILE_NAME.sha256")
        val verifiedHash = verificationFile.takeIf(File::isFile)?.readText()?.trim()
        if (
            modelFile.isFile &&
            modelFile.length() == MODEL_FILE_BYTES &&
            verifiedHash.equals(MODEL_SHA256, ignoreCase = true)
        ) {
            return modelFile
        }

        if (modelDirectory.usableSpace < MODEL_FILE_BYTES + MODEL_COPY_HEADROOM_BYTES) {
            throw IllegalStateException("There is not enough free storage to prepare the on-device model.")
        }

        val temporaryFile = File(modelDirectory, "$MODEL_FILE_NAME.copying")
        val temporaryVerificationFile = File(modelDirectory, "$MODEL_FILE_NAME.sha256.copying")
        temporaryFile.delete()
        temporaryVerificationFile.delete()

        try {
            val digest = MessageDigest.getInstance("SHA-256")
            context.assets.open(MODEL_ASSET, android.content.res.AssetManager.ACCESS_STREAMING).use { input ->
                FileOutputStream(temporaryFile).buffered(MODEL_COPY_BUFFER_BYTES).use { output ->
                    val buffer = ByteArray(MODEL_COPY_BUFFER_BYTES)
                    var copiedBytes = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        digest.update(buffer, 0, count)
                        copiedBytes += count
                    }
                    if (copiedBytes != MODEL_FILE_BYTES) {
                        throw IllegalStateException("The bundled model has an unexpected size.")
                    }
                }
            }

            val copiedHash = digest.digest().joinToString("") { byte -> "%02X".format(byte.toInt() and 0xFF) }
            if (!copiedHash.equals(MODEL_SHA256, ignoreCase = true)) {
                throw IllegalStateException("The bundled model failed its integrity check.")
            }

            temporaryVerificationFile.writeText(MODEL_SHA256)
            moveReplacing(temporaryFile, modelFile)
            moveReplacing(temporaryVerificationFile, verificationFile)
            return modelFile
        } finally {
            temporaryFile.delete()
            temporaryVerificationFile.delete()
        }
    }

    private fun moveReplacing(source: File, destination: File) {
        try {
            Files.move(
                source.toPath(),
                destination.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun preprocess(bitmap: Bitmap): FloatArray {
        val scaled = Bitmap.createScaledBitmap(bitmap, MODEL_SIZE, MODEL_SIZE, true)
        val pixels = IntArray(MODEL_SIZE * MODEL_SIZE)
        scaled.getPixels(pixels, 0, MODEL_SIZE, 0, 0, MODEL_SIZE, MODEL_SIZE)
        if (scaled !== bitmap) scaled.recycle()

        val plane = MODEL_SIZE * MODEL_SIZE
        val tensor = FloatArray(plane * 3)
        for (index in 0 until plane) {
            val pixel = pixels[index]
            tensor[index] = ((Color.red(pixel) / 255f) - MEAN[0]) / STD[0]
            tensor[plane + index] = ((Color.green(pixel) / 255f) - MEAN[1]) / STD[1]
            tensor[(2 * plane) + index] = ((Color.blue(pixel) / 255f) - MEAN[2]) / STD[2]
        }
        return tensor
    }

    private fun runInference(input: FloatArray): ByteArray {
        val inputName = session.inputNames.singleOrNull()
            ?: throw InferenceException("The AI model has an unexpected input signature.")
        OnnxTensor.createTensor(
            environment,
            FloatBuffer.wrap(input),
            longArrayOf(1, 3, MODEL_SIZE.toLong(), MODEL_SIZE.toLong()),
        ).use { tensor ->
            session.run(mapOf(inputName to tensor)).use { result ->
                val output = result[0] as? OnnxTensor
                    ?: throw InferenceException("The AI model returned an invalid result.")
                val count = output.info.shape.fold(1L) { total, value -> total * value }.toInt()
                if (count != MODEL_SIZE * MODEL_SIZE) {
                    throw InferenceException("The AI model returned an unexpected matte size.")
                }
                val buffer = output.floatBuffer
                    ?: throw InferenceException("The AI model returned an unsupported matte format.")
                val logits = FloatArray(count)
                buffer.get(logits)
                return MatteProcessor.logitsToAlpha(logits)
            }
        }
    }

    private fun composite(source: Bitmap, alpha: ByteArray): Bitmap {
        val maskPixels = IntArray(alpha.size) { index ->
            val value = alpha[index].toInt() and 0xFF
            Color.argb(value, 255, 255, 255)
        }
        val mask = Bitmap.createBitmap(maskPixels, MODEL_SIZE, MODEL_SIZE, Bitmap.Config.ARGB_8888)
        val output = source.copy(Bitmap.Config.ARGB_8888, true)
            ?: throw InferenceException("The transparent result could not be allocated.")
        output.setHasAlpha(true)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }
        canvas.drawBitmap(mask, null, android.graphics.Rect(0, 0, output.width, output.height), paint)
        paint.xfermode = null
        mask.recycle()
        return output
    }

    companion object {
        const val MODEL_NAME = "BiRefNet Lite 512"
        const val MODEL_LICENSE = "MIT"
        const val MODEL_ASSET = "models/birefnet-lite-512-fp16.onnx"
        const val MODEL_SIZE = 512
        private const val MODEL_FILE_NAME = "birefnet-lite-512-fp16.onnx"
        private const val MODEL_FILE_BYTES = 98_484_532L
        private const val MODEL_SHA256 = "EFF9216BB2F9D3F023D9C2B7196845A7485739AB1F231593633E4D2344FFC516"
        private const val MODEL_COPY_BUFFER_BYTES = 1024 * 1024
        private const val MODEL_COPY_HEADROOM_BYTES = 8L * 1024L * 1024L
        private const val TAG = "BackgroundRemover"
        private val MEAN = floatArrayOf(0.485f, 0.456f, 0.406f)
        private val STD = floatArrayOf(0.229f, 0.224f, 0.225f)
    }
}

class InferenceException(message: String, cause: Throwable? = null) : Exception(message, cause)
