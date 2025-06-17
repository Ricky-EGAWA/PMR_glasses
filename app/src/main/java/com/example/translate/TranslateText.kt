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
class TranslateText(private val targetLanguage: String) {
    private val TAG = "TranslateText"
    fun translate(originText: String,onTranslateSuccess:(String)-> Unit){
        //identify the origin text language
        identifyLanguage(originText){ originLangTag ->
            if (originLangTag == "null" || originLangTag == "und"){
                Log.e(TAG,"Stop translate because of the error on origin text language")
            }else {
                // Create an translator:
                val options = TranslatorOptions.Builder()
                    .setSourceLanguage(TranslateLanguage.fromLanguageTag(originLangTag).toString())
                    .setTargetLanguage(targetLanguage)
                    .build()
                val translator = Translation.getClient(options)
                // download needed model
                var conditions = DownloadConditions.Builder()
                    .requireWifi()
                    .build()
                translator.downloadModelIfNeeded(conditions)
                    .addOnSuccessListener {
                        // Model downloaded successfully. Okay to start translating.
                        Log.i(TAG,"Model downloaded successfully")
                        //translate
                        translator.translate(originText)
                            .addOnSuccessListener { translatedText ->
                                // Translation successful.
                                Log.i(TAG,"Translation successful")
                                Log.i(TAG,"Translated text:${translatedText}")
                                onTranslateSuccess(translatedText)
                                translator.close()
                            }
                            .addOnFailureListener { exception ->
                                // Error.
                                Log.e(TAG,"Translation failed:${exception}")
                                translator.close()
                            }
                    }
                    .addOnFailureListener { exception ->
                        // Model couldn’t be downloaded or other internal error.
                        Log.e(TAG,"Model downloaded failed")
                        translator.close()
                    }


            }

        }

    }


    private fun identifyLanguage(text: String,onSuccess:(String)-> Unit){
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
                // Model couldn’t be loaded or other internal error.
                Log.e(TAG,"Error for the language identify service")
            }

    }
}