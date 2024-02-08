package com.ogzkesk.tfliteimageclassification.helper

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import com.ogzkesk.tfliteimageclassification.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import org.tensorflow.lite.task.vision.detector.ObjectDetector.ObjectDetectorOptions


class ObjectDetectorHelper(
    private val context: Context,
    var currentDelegate: Int = DELEGATE_CPU,
    var currentModel: Int = MODEL_EFFICIENTNETV0,
    val maxResults: Int = 3,
    private val listener: DetectionListener?,
) {


    interface DetectionListener {
        fun onStart()
        fun onError(error: String?)
        fun onResult(results: List<Detection>, inferenceTime: Long)
    }

    private var objectDetector: ObjectDetector? = null

    init {
        initDetector()
    }

    private fun initDetector() {
        try {
            objectDetector = ObjectDetector
                .createFromFileAndOptions(context, getModelName(), getOptions())
        } catch (e: Exception) {
            listener?.onError(e.message)
            Logger.log(e)
        }
    }

    suspend fun detect(image: Bitmap) {
        withContext(Dispatchers.IO) {
            if(objectDetector == null){
                initDetector()
            }

            listener?.onStart()
            var inferenceTime = SystemClock.uptimeMillis()
            val tensorImage = TensorImage.fromBitmap(image)

            try {
                val results: List<Detection> = objectDetector?.detect(tensorImage) ?: emptyList()
                inferenceTime = SystemClock.uptimeMillis() - inferenceTime
                listener?.onResult(results, inferenceTime)
            } catch (e: Exception) {
                listener?.onError(e.message)
            }
        }
    }

    private fun getOptions(): ObjectDetectorOptions {
        val baseOptions = when(currentDelegate){
            DELEGATE_CPU -> BaseOptions.builder()
                .setNumThreads(10)
                .build()
            DELEGATE_NNAPI -> BaseOptions.builder()
                .setNumThreads(10)
                .useNnapi()
                .build()
            DELEGATE_GPU -> {
                if (CompatibilityList().isDelegateSupportedOnThisDevice) {
                    BaseOptions.builder()
                        .setNumThreads(10)
                        .useGpu()
                        .build()
                } else {
                    listener?.onError("GPU is not supported on this device")
                    Logger.log("GPU is not supported on this device")
                    BaseOptions.builder()
                        .setNumThreads(10)
                        .build()
                }
            }
            else -> BaseOptions.builder()
                .setNumThreads(10)
                .build()
        }

        return ObjectDetectorOptions.builder()
            .setBaseOptions(baseOptions)
            .setMaxResults(maxResults)
            .build()
    }

    private fun getModelName(): String {
        return when (currentModel) {
            MODEL_EFFICIENTNETV0 -> "efficientnet-lite0.tflite"
            MODEL_EFFICIENTNETV1 -> "efficientnet-lite1.tflite"
            MODEL_EFFICIENTNETV2 -> "efficientnet-lite2.tflite"
            MODEL_EFFICIENTNETV3 -> "efficientnet-lite3.tflite"
            MODEL_EFFICIENTNETV3X -> "efficientnet-lite3x.tflite"
            MODEL_EFFICIENTNETV4 -> "efficientnet-lite4.tflite"
            MODEL_MOBILENETV1 -> "ssd-mobilenet-v1.tflite"
            MODEL_MOBILENETV2 -> "mobilenet_v2.tflite"
            else -> "efficientnet-lite0.tflite"
        }
    }

    companion object {
        const val DELEGATE_CPU = 0
        const val DELEGATE_GPU = 1
        const val DELEGATE_NNAPI = 2
        const val MODEL_EFFICIENTNETV0 = 0
        const val MODEL_EFFICIENTNETV1 = 1
        const val MODEL_EFFICIENTNETV2 = 2
        const val MODEL_EFFICIENTNETV3 = 3
        const val MODEL_EFFICIENTNETV3X = 4
        const val MODEL_EFFICIENTNETV4 = 5
        const val MODEL_MOBILENETV1 = 6
        const val MODEL_MOBILENETV2 = 7
    }
}