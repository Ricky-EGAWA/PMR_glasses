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

/**
 * A utility class responsible for handling image input from shared intents and performing
 * text recognition using Google ML Kit.
 */
class RecognizeText() {
    private val TAG = "RecognizeText"

    /**
     * Handles incoming Intent data, specifically for image sharing actions (ACTION_SEND,
     * ACTION_SEND_MULTIPLE), to extract and return a list of image URIs.
     * It filters for image types and logs errors if no image data is found or the intent is not
     * a sharing intent.
     *
     * @param intent The Intent received by the activity, potentially containing shared image data.
     * @return A list of Uri objects representing the shared images, or an empty list if none found.
     */
    fun getImageUri(intent: Intent): List<Uri> {
        // check Intent if is ACTION_SEND
        when (intent?.action) {
            Intent.ACTION_SEND -> {
                // Check if the received data type is an image.
                if (intent.type?.startsWith("image/") == true) {
                    val uri = handleSendImage(intent)
                    if (uri != null) return listOf(uri)
                } else {
                    Log.e(TAG, "not image data")
                    return emptyList<Uri>()
                }
            }

            Intent.ACTION_SEND_MULTIPLE -> {
                // Handle multiple image sharing.
                if (intent.type?.startsWith("image/") == true) {
                    return handleSendMultipleImages(intent)
                } else {
                    Log.e(TAG, "not image data")
                    return emptyList<Uri>()
                }
            }

            else -> {
                Log.e(TAG, "not get shared data")
                return emptyList<Uri>()
            }
        }
        return emptyList()
    }

    /**
     * A helper function to extract a single image URI from an ACTION_SEND Intent's EXTRA_STREAM.
     * Logs whether the URI was found or not.
     *
     * @param intent The ACTION_SEND Intent.
     * @return The Uri of the shared image, or null if not found.
     */
    private fun handleSendImage(intent: Intent): Uri? {
        (intent.parcelable<Uri>(Intent.EXTRA_STREAM))?.let { imageUri ->
            Log.i(TAG, "received image URI: $imageUri")
            return imageUri
        } ?: run {
            Log.e(TAG, "can not find image URI")
            return null
        }
    }

    /**
     * A helper function to extract a list of image URIs from an ACTION_SEND_MULTIPLE Intent's EXTRA_STREAM.
     * Logs the number of images received or if the list is empty.
     *
     * @param intent The ACTION_SEND_MULTIPLE Intent.
     * @return A list of Uri objects for the shared images, or an empty list if none found.
     */
    private fun handleSendMultipleImages(intent: Intent): List<Uri> {
        val imageUris = intent.parcelableArrayList<Uri>(Intent.EXTRA_STREAM)
        if (!imageUris.isNullOrEmpty()) {
            Log.i(TAG, "receive ${imageUris.size} images")
            return imageUris.toList()
        } else {
            Log.e(TAG, "can not find image URI list")
            return emptyList<Uri>()
        }
    }

    /**
     * A utility function to demonstrate how to access detailed information (text, bounding boxes,
     * corner points) from the result of text recognition (ML Kit's 'Text' object).
     * It iterates through blocks, lines, and individual elements within the recognized text structure.
     * (Note: This function currently only accesses data and does not perform any specific action
     * like displaying it).
     *
     * @param result The Text object returned by ML Kit's Text Recognition API.
     */
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

    /**
     * Performs text recognition on a given image URI using Google ML Kit.
     * It selects the appropriate text recognizer (Chinese, Latin, or Japanese) based on the
     * 'lang' parameter, processes the image, and then invokes the 'onTextRecognized' callback
     * with the results or logs any errors.
     *
     * @param lang The target script language for recognition ("zh" for Chinese, "la" for Latin, "jp" for Japanese).
     * @param context The Android Context, used to load the image from the URI.
     * @param uri The Uri of the image to be processed.
     * @param onTextRecognized A callback function that receives the recognized Text object upon success.
     */
    fun recognizeText(lang: String, context: Context, uri: Uri, onTextRecognized: (Text) -> Unit) {
        // Selects the appropriate ML Kit TextRecognizer client based on the specified language.
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

// --- Extension Functions for Intent Parcelable Handling ---
// These extension functions provide a safe and backward-compatible way to retrieve Parcelable
// objects and ArrayLists from an Intent, adapting to changes in Android's API (specifically
// for Android 13/API 33 - TIRAMISU and above).

/**
 * Extension function for Intent to safely retrieve a single Parcelable extra.
 * It uses the newer `getParcelableExtra(key, Class)` method for Android TIRAMISU (API 33+) and above,
 * and falls back to the deprecated `getParcelableExtra(key)` for older versions to ensure compatibility.
 *
 * @param key The name of the extra.
 * @return The Parcelable object, or null if not found or if the type doesn't match.
 */
inline fun <reified T : Parcelable> Intent.parcelable(key: String): T? = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getParcelableExtra(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelableExtra(key) as? T // suppress deprecation warning for older versions
}

/**
 * Extension function for Intent to safely retrieve a list of Parcelable extras.
 * Similar to `parcelable`, it uses the newer `getParcelableArrayListExtra(key, Class)` method
 * for Android TIRAMISU (API 33+) and above, and the deprecated `getParcelableArrayListExtra(key)`
 * for older versions.
 *
 * @param key The name of the extra.
 * @return The ArrayList of Parcelable objects, or null if not found or if the type doesn't match.
 */
inline fun <reified T : Parcelable> Intent.parcelableArrayList(key: String): ArrayList<T>? = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getParcelableArrayListExtra(
        key,
        T::class.java
    )

    else -> @Suppress("DEPRECATION") getParcelableArrayListExtra<T>(key) as? ArrayList<T> // suppress deprecation warning
}