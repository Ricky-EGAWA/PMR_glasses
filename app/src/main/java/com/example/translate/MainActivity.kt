package com.example.translate

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.translate.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.mlkit.nl.translate.TranslateLanguage
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val RECORD_AUDIO_PERMISSION = 1000

    private var isRecording = false
    private var audioRecord: AudioRecord? = null
    private lateinit var recordButton: ImageButton

    private val apiKey = "AIzaSyBMyqt2BcW2L3Uh8a_c1u0t6D4M5W3Vqdw" // ← APIキーは適宜置き換えてください

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // パーミッションチェック
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_PERMISSION)
        }

        recordButton = binding.recordButton

        recordButton.setOnClickListener {
            if (!isRecording) {
                startRecording()
            } else {
                stopRecording()
            }
        }

        //translate test
        //low quality
        val toEnglishTranslator = TranslateText(targetLanguage = TranslateLanguage.ENGLISH)
        toEnglishTranslator.translate("测试"){}
        toEnglishTranslator.translate("这个节目也是挺搞笑的，不知道下一集什么时候更新") {  }

    }

    private fun sendToSpeechToTextApi(audioData: ByteArray, apiKey: String) {
        val base64Audio = Base64.encodeToString(audioData, Base64.NO_WRAP)

        val json = """
        {
          "config": {
            "encoding": "LINEAR16",
            "sampleRateHertz": 16000,
            "languageCode": "ja-JP"
          },
          "audio": {
            "content": "$base64Audio"
          }
        }
    """.trimIndent()

        val client = OkHttpClient()
        val requestBody = json.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("https://speech.googleapis.com/v1/speech:recognize?key=$apiKey")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("SpeechAPI", "通信エラー: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                Log.d("SpeechAPI", "レスポンス: $body")

                if (body != null) {
                    try {
                        val json = JSONObject(body)
                        val results = json.getJSONArray("results")
                        if (results.length() > 0) {
                            val alternatives = results.getJSONObject(0).getJSONArray("alternatives")
                            if (alternatives.length() > 0) {
                                val transcript = alternatives.getJSONObject(0).getString("transcript")

                                // 翻訳処理
                                val translator = TranslateText(targetLanguage = TranslateLanguage.CHINESE)
                                translator.translate(transcript) { translatedText ->
                                    runOnUiThread {
                                        binding.resultText.text =
                                            "[原文]\n$transcript\n\n[翻訳]\n$translatedText"
                                    }
                                }
                            }
                        }
                    } catch (e: JSONException) {
                        Log.e("SpeechAPI", "JSON解析エラー: ${e.message}")
                    }
                }
            }
        })
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "マイク使用が許可されました", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "マイク使用が拒否されました", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_PERMISSION)
            return
        }

        isRecording = true
        runOnUiThread {
            recordButton.setImageResource(R.drawable.stop_24px)  // 停止アイコンに切り替え
        }

        // AudioRecord初期化など録音開始処理
        val sampleRate = 16000
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )
        audioRecord?.startRecording()

        // 録音を別スレッドで行いバッファに貯める処理を開始
        Thread {
            val audioData = ByteArray(sampleRate * 2 * 5) // 5秒分のバッファ
            var offset = 0
            while (isRecording && offset < audioData.size) {
                val read = audioRecord?.read(audioData, offset, audioData.size - offset) ?: 0
                if (read > 0) {
                    offset += read
                }
            }
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null

            if (offset > 0) {
                val actualAudio = audioData.copyOf(offset)
                sendToSpeechToTextApi(actualAudio, apiKey)
            }

            isRecording = false
            runOnUiThread {
                recordButton.setImageResource(R.drawable.mic_24px)  // マイクアイコンに戻す
            }
        }.start()
    }

    private fun stopRecording() {
        isRecording = false
        // スレッドは自動的に録音停止して終了します
    }
}
