package com.example.translate

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Base64
import android.util.Log
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
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val RECORD_AUDIO_PERMISSION = 1000
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

        // ボタンのクリックリスナー
        binding.recordButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED
            ) {
                Thread {
                    try {
                        val audioData = recordAudio()
                        sendToSpeechToTextApi(audioData, apiKey)
                    } catch (e: SecurityException) {
                        Log.e("Permission", "録音の権限がありません: ${e.message}")
                    }
                }.start()
            } else {
                Toast.makeText(this, "マイクの使用許可が必要です", Toast.LENGTH_SHORT).show()
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    RECORD_AUDIO_PERMISSION
                )
            }
        }
    }

    private fun recordAudio(durationSec: Int = 5): ByteArray {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            throw SecurityException("録音のパーミッションがありません")
        }

        val sampleRate = 16000
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        val audioData = ByteArray(sampleRate * 2 * durationSec)
        try {
            audioRecord.startRecording()
            audioRecord.read(audioData, 0, audioData.size)
        } finally {
            audioRecord.stop()
            audioRecord.release()
        }

        return audioData
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

                                // UIスレッドでTextViewにセット
                                runOnUiThread {
                                    binding.resultText.text = transcript
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
}
