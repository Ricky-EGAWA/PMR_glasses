package com.example.translate

import android.util.Log
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions

//use Google ML Kit to translate
//test for translate quality
//low quality
/**
 * A utility class for text translation using Google ML Kit's On-device Translation API.
 * It identifies the source language automatically and translates text to a predefined target language.
 *
 * @param targetLanguage The BCP-47 language tag of the language to translate to (e.g., "en", "es", "zh").
 */
class TranslateText(private val targetLanguage: String) {
    private val TAG = "TranslateText"

    /**
     * Translates the given `originText` to the `targetLanguage`.
     * This function first identifies the language of the `originText`, then attempts to download
     * the necessary translation model (if not already present), and finally performs the translation.
     * The `onTranslateSuccess` callback is invoked with the translated text upon successful completion.
     *
     * @param originText The text string to be translated.
     * @param onTranslateSuccess A callback function that receives the translated text upon success.
     */
    fun translate(originText: String, onTranslateSuccess: (String) -> Unit) {
        //identify the origin text language
        identifyLanguage(originText) { originLangTag ->
            if (originLangTag == "null" || originLangTag == "und") {
                Log.e(TAG, "Stop translate because of the error on origin text language")
            } else {
                // --- ML Kit Translator Setup ---
                // Create an translator:
                val options = TranslatorOptions.Builder()
                    .setSourceLanguage(TranslateLanguage.fromLanguageTag(originLangTag).toString())
                    .setTargetLanguage(targetLanguage)
                    .build()
                val translator = Translation.getClient(options)
                // --- Model Download ---
                // download needed model
                var conditions = DownloadConditions.Builder()
                    .requireWifi()
                    .build()
                translator.downloadModelIfNeeded(conditions)
                    .addOnSuccessListener {
                        // Model downloaded successfully. Okay to start translating.
                        Log.i(TAG, "Model downloaded successfully")
                        // --- Perform Translation ---
                        //translate
                        translator.translate(originText)
                            .addOnSuccessListener { translatedText ->
                                // Translation successful.
                                Log.i(TAG, "Translation successful")
                                Log.i(TAG, "Translated text:${translatedText}")
                                onTranslateSuccess(translatedText)
                                translator.close()
                            }
                            .addOnFailureListener { exception ->
                                // Error.
                                Log.e(TAG, "Translation failed:${exception}")
                                translator.close()
                            }
                    }
                    .addOnFailureListener { exception ->
                        // Model couldn’t be downloaded or other internal error.
                        Log.e(TAG, "Model downloaded failed")
                        translator.close()
                    }


            }

        }

    }

    /**
     * Identifies the language of the given text using Google ML Kit's Language Identification API.
     * The `onSuccess` callback is invoked with the identified language code (BCP-47 tag) or "und"
     * if the language cannot be determined.
     *
     * @param text The string whose language needs to be identified.
     * @param onSuccess A callback function that receives the identified language code.
     */
    private fun identifyLanguage(text: String, onSuccess: (String) -> Unit) {
        val languageIdentifier = LanguageIdentification.getClient()
        languageIdentifier.identifyLanguage(text)
            .addOnSuccessListener { languageCode ->
                if (languageCode == "und") {
                    Log.i(TAG, "Can't identify language.")
                    onSuccess(languageCode)
                } else {
                    Log.i(TAG, "Language: $languageCode")
                    onSuccess(languageCode)
                }
            }
            .addOnFailureListener {
                // Language identification failed (e.g., model couldn't be loaded).
                // Model couldn’t be loaded or other internal error.
                Log.e(TAG, "Error for the language identify service")
            }

    }
}