package com.dicoding.testmata

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import android.view.Surface
import org.tensorflow.lite.support.image.ImageProcessor
import androidx.camera.core.ImageProxy
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.core.vision.ImageProcessingOptions
import org.tensorflow.lite.task.vision.classifier.Classifications
import org.tensorflow.lite.task.vision.classifier.ImageClassifier
import java.lang.IllegalStateException


class ImageClassifierHelper (
    var threshold: Float = 0.1f,
    var maxResults: Int = 3,
    val modelName: String = "keratitis_metadata.tflite",
    val context: Context,
    val classifierListener: ClassifierListener?
) {
    private var imageClassifier: ImageClassifier? = null

    init {
        setupImageClassifier()
    }

    private fun setupImageClassifier(){
        val optionsBuilder = ImageClassifier.ImageClassifierOptions.builder()
            .setScoreThreshold(threshold)
            .setMaxResults(maxResults)

        val baseOptionBuilder = BaseOptions.builder()
            .setNumThreads(4)
        optionsBuilder.setBaseOptions(baseOptionBuilder.build())

        try {
            imageClassifier = ImageClassifier.createFromFileAndOptions(
                context,
                modelName,
                optionsBuilder.build()
            )
        } catch (e: IllegalStateException){
            classifierListener?.onError("Image Failed")
            Log.e(TAG, e.message.toString())
        }
    }

    fun classifyImage(image: Uri) {
        if (imageClassifier == null){
            setupImageClassifier()
        }

        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(96, 183, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
            .add(CastOp(DataType.FLOAT32))
            .build()

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
            val source = ImageDecoder.createSource(context.contentResolver, image)
            ImageDecoder.decodeBitmap(source)
        } else {
            MediaStore.Images.Media.getBitmap(context.contentResolver, image)
        } .copy(Bitmap.Config.ARGB_8888, true)?.let { bitmap ->
            val tensorImage = imageProcessor.process(TensorImage.fromBitmap(bitmap))
            val imageProcessingOptions = ImageProcessingOptions.builder()
                .build()

            var inferenceTime = SystemClock.uptimeMillis()
            val results = imageClassifier?.classify(tensorImage, imageProcessingOptions)
            inferenceTime = SystemClock.uptimeMillis() - inferenceTime
            classifierListener?.onResults(
                results,
                inferenceTime
            )

        }


    }

    private fun toBitmap(image: ImageProxy): Bitmap {
        val bitmapBuffer = Bitmap.createBitmap(
            image.width,
            image.height,
            Bitmap.Config.ARGB_8888
        )
        image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer) }
        image.close()
        return bitmapBuffer
    }

    private fun getOrientationFromRotation(rotation: Int): ImageProcessingOptions.Orientation {
        return when (rotation) {
            Surface.ROTATION_270 -> ImageProcessingOptions.Orientation.BOTTOM_RIGHT
            Surface.ROTATION_180 -> ImageProcessingOptions.Orientation.RIGHT_BOTTOM
            Surface.ROTATION_90 -> ImageProcessingOptions.Orientation.TOP_LEFT
            else -> ImageProcessingOptions.Orientation.RIGHT_TOP
        }
    }

    interface ClassifierListener {
        fun onError(error: String)
        fun onResults(
            results: List<Classifications>?,
            infereceTime: Long
            )
    }
    companion object {
        private const val TAG = "ImageClassidierHelper"
    }
}