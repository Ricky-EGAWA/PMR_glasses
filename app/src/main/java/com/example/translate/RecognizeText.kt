package com.example.translate

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.TextView
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import android.widget.ImageView
import com.google.mlkit.vision.common.InputImage


class RecognizeText() {
    private val TAG = "RecognizeText"

    fun getImageUri(intent: Intent): List<Uri> {
        // 检查 Intent 是否是 ACTION_SEND
        when (intent?.action) {
            Intent.ACTION_SEND -> {
                // 确保数据类型是图片
                if (intent.type?.startsWith("image/") == true) {
                    val uri = handleSendImage(intent)
                    if (uri != null) return listOf(uri)
                } else {
                    Log.e(TAG, "收到的不是图片数据")
                    return emptyList<Uri>()
                }
            }

            Intent.ACTION_SEND_MULTIPLE -> {
                // 接收多张图片
                if (intent.type?.startsWith("image/") == true) {
                    return handleSendMultipleImages(intent)
                } else {
                    Log.e(TAG, "收到的不是图片数据")
                    return emptyList<Uri>()
                }
            }

            else -> {
                Log.e(TAG, "没有收到分享数据")
                return emptyList<Uri>()
            }
        }
        return emptyList()
    }

    private fun handleSendImage(intent: Intent): Uri? {
        (intent.parcelable<Uri>(Intent.EXTRA_STREAM))?.let { imageUri ->
            Log.i(TAG, "接收到图片URI: $imageUri")
//            try {
//                // 使用 ContentResolver 从 URI 读取图片数据
//                context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
//                    val bitmap = BitmapFactory.decodeStream(inputStream)
//                    imageView.setImageBitmap(bitmap)
//                    // 在这里你可以将图片保存到本地存储
//                    // 例如：saveImageToInternalStorage(bitmap, "received_image.jpeg")
//                }
//            } catch (e: Exception) {
//                statusText.text = "无法读取图片: ${e.message}"
//                e.printStackTrace()
//            }
            return imageUri
        } ?: run {
            Log.e(TAG, "未找到图片URI")
            return null
        }
    }

    private fun handleSendMultipleImages(intent: Intent): List<Uri> {
        val imageUris = intent.parcelableArrayList<Uri>(Intent.EXTRA_STREAM)
        if (!imageUris.isNullOrEmpty()) {
            Log.i(TAG, "接收到 ${imageUris.size} 张图片")
            // 这里你可以遍历 imageUris 并逐个处理
            // 比如只显示第一张图片
            return imageUris.toList()
//            handleSendImage(Intent().apply {
//                action = Intent.ACTION_SEND
//                type = "image/jpeg"
//                putExtra(Intent.EXTRA_STREAM, imageUris[0])
//            })
        } else {
            Log.e(TAG, "未找到图片URI列表")
            return emptyList<Uri>()
        }
    }

    private fun getTextInfo(result: Text) {
        val resultText = result.text
        for (block in result.textBlocks) {
            val blockText = block.text
            val blockCornerPoints = block.cornerPoints
            val blockFrame = block.boundingBox
            for (line in block.lines) {
                val lineText = line.text
                val lineCornerPoints = line.cornerPoints
                val lineFrame = line.boundingBox
                for (element in line.elements) {
                    val elementText = element.text
                    val elementCornerPoints = element.cornerPoints
                    val elementFrame = element.boundingBox
                }
            }
        }
    }

    fun recognizeText(lang: String, context: Context, uri: Uri, onTextRecognized: (Text) -> Unit) {
        val recognizer: TextRecognizer = when (lang) {
            // When using Chinese script library
            "zh" -> TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
            // When using Latin script library
            "la" -> TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            // When using Japanese script library
            "jp" -> TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
            else -> {
                Log.e(TAG, "wrong choice of the target language (not zh/la/jp)")
                return
            }
        }
        val image: InputImage
        try {
            image = InputImage.fromFilePath(context, uri)
            val result = recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    // Task completed successfully
                    onTextRecognized(visionText)
                }
                .addOnFailureListener { e ->
                    // Task failed with an exception
                    Log.e(TAG, e.toString())
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }
}


// 针对单个 Parcelable 的扩展函数
inline fun <reified T : Parcelable> Intent.parcelable(key: String): T? = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getParcelableExtra(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelableExtra(key) as? T // suppress deprecation warning for older versions
}

// 针对 Parcelable ArrayList 的扩展函数
inline fun <reified T : Parcelable> Intent.parcelableArrayList(key: String): ArrayList<T>? = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getParcelableArrayListExtra(
        key,
        T::class.java
    )

    else -> @Suppress("DEPRECATION") getParcelableArrayListExtra<T>(key) as? ArrayList<T> // suppress deprecation warning
}