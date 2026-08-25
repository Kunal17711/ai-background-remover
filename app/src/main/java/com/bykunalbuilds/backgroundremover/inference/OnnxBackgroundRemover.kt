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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.nio.FloatBuffer
import java.nio.channels.FileChannel
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
            val options = OrtSession.SessionOptions().apply {
                setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
                setIntraOpNumThreads(min(4, Runtime.getRuntime().availableProcessors().coerceAtLeast(2)))
                setInterOpNumThreads(1)
            }
            context.assets.openFd(MODEL_ASSET).use { descriptor ->
                FileInputStream(descriptor.fileDescriptor).channel.use { channel ->
                    val model = channel.map(
                        FileChannel.MapMode.READ_ONLY,
                        descriptor.startOffset,
                        descriptor.declaredLength,
                    )
                    return environment.createSession(model, options)
                }
            }
        } catch (error: Exception) {
            throw InferenceException("The on-device AI model could not be loaded.", error)
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
        private val MEAN = floatArrayOf(0.485f, 0.456f, 0.406f)
        private val STD = floatArrayOf(0.229f, 0.224f, 0.225f)
    }
}

class InferenceException(message: String, cause: Throwable? = null) : Exception(message, cause)
