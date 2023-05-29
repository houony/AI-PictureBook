package com.example.myapplication

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.example.myapplication.databinding.ActivitySettingBinding
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class SettingActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingBinding
    private var numMan = 0
    private var numWoman = 0

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "그림책 만들기"

        val outputMan = findViewById<TextView>(R.id.NumMan)
        val outputWoman = findViewById<TextView>(R.id.NumWoman)

        updateOutputText(outputMan, numMan)
        updateOutputText(outputWoman, numWoman)

        binding.minusMan.setOnClickListener {
            if (numMan > 0) {
                numMan--
            }
            updateOutputText(outputMan, numMan)
        }

        binding.plusMan.setOnClickListener {
            numMan++
            updateOutputText(outputMan, numMan)
        }

        binding.minusWoman.setOnClickListener {
            if (numWoman > 0) {
                numWoman--
            }
            updateOutputText(outputWoman, numWoman)
        }

        binding.plusWoman.setOnClickListener {
            numWoman++
            updateOutputText(outputWoman, numWoman)
        }

        binding.btnwrite.setOnClickListener {
            val selectedGenre = binding.genre.selectedItem.toString()
            val selectedEra = binding.era.selectedItem.toString()
            val writesumText = binding.writesum.text.toString()

            if (writesumText.length > 200) {
                Toast.makeText(this@SettingActivity, "글자 수를 200자 이내로 제한해 주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            var customwritesumText: String

            translateToEnglish(writesumText) { translatedText ->
                customwritesumText = translatedText

                val intent: Intent = Intent(this@SettingActivity, LoadingActivity::class.java)
                startActivity(intent)

                val customGenre = when (selectedGenre) { // 장르 영어로 변경
                    "로맨스" -> "Romance"
                    "판타지" -> "Fantasy"
                    "공상 과학" -> "science fiction"
                    "스포츠" -> "sports"
                    else -> "Feel free to fill it out" // 선택한 장르에 해당하지 않는 경우의 기본값 설정
                }

                val customEra = when (selectedEra) { // 시대 영어로 변경
                    "현대" -> "Modern "
                    "미래" -> "Future"
                    "19세기" -> "19th century"
                    "르네상스" -> "the Renaissance"
                    else -> "Feel free to fill it out" // 선택한 시대에 해당하지 않는 경우의 기본값 설정
                }

                runGPT3(customGenre, customEra, numMan, numWoman, customwritesumText) { responseBody ->
                    val originalResponseBody = responseBody
                    translateToKorean(responseBody) { translatedResponseBody ->
                        Log.d("Response Body:", responseBody)
                        Log.d("TranslatedRespons:", translatedResponseBody)
                        val intent = Intent(this@SettingActivity, SubActivity::class.java).apply {
                            putExtra("next", "level")
                            putExtra("selectedGenre", customGenre)
                            putExtra("selectedEra", customEra)
                            putExtra("NumMan", numMan)
                            putExtra("NumWoman", numWoman)
                            putExtra("num", 30)
                            putExtra("key", customwritesumText)
                            putExtra("summary", translatedResponseBody)
                            putExtra("originalsummary", originalResponseBody) // 번역 전의 텍스트 인텐트에 추가
                        }
                        startActivity(intent)
                    }
                }
            }
        }
    }

    private fun translateToEnglish(inputText: String, callback: (String) -> Unit) {
        val client = OkHttpClient.Builder().build()
        val url = "https://openapi.naver.com/v1/papago/n2mt"

        val requestBody = FormBody.Builder()
            .add("source", "ko")
            .add("target", "en")
            .add("text", inputText)
            .build()

        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            .addHeader("X-Naver-Client-Id", "Client_ID")
            .addHeader("X-Naver-Client-Secret", "Client_Secret")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Translation request failed
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (body != null) {
                    val jsonObject = JSONObject(body)
                    val translatedText = jsonObject.getJSONObject("message")
                        .getJSONObject("result")
                        .getString("translatedText")

                    callback(translatedText)
                }
            }
        })
    }

    private fun translateToKorean(inputText: String, callback: (String) -> Unit) {
        val client = OkHttpClient.Builder().build()
        val url = "https://openapi.naver.com/v1/papago/n2mt"

        val requestBody = FormBody.Builder()
            .add("source", "en")
            .add("target", "ko")
            .add("text", inputText)
            .build()

        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            .addHeader("X-Naver-Client-Id", "Client_ID")
            .addHeader("X-Naver-Client-Secret", "Client_Secret")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Translation request failed
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (body != null) {
                    val jsonObject = JSONObject(body)
                    val translatedText = jsonObject.getJSONObject("message")
                        .getJSONObject("result")
                        .getString("translatedText")

                    callback(translatedText)
                }
            }
        })
    }

    private fun updateOutputText(textView: TextView, value: Int) {
        textView.text = value.toString()
    }

    private fun runGPT3(
        customGenre: String,
        customEra: String,
        numMan: Int,
        numWoman: Int,
        customwritesumText: String,
        callback: (String) -> Unit
    ) {

        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val apiKey = "mykey"
        val url = "https://api.openai.com/v1/chat/completions"

        val requestBody = """
        {
         "model": "gpt-3.5-turbo",
            "messages": [
                {"role": "system", "content": "The following are the modified rules for creating a logical and coherent novel."},
                {"role": "system", "content": "write the title. form is title: title"},
                {"role": "system", "content": "Limit novels to a maximum of 10 sentences. The sentence should be short and simple."},
                {"role": "system", "content": "Every novel's sentence should be clearly written, and the story should be smooth without any illogical or inconsistent elements."},
                {"role": "user", "content": "the number of male characters: $numMan, the number of female characters: $numWoman, the novel's background period: $customEra, a novel genre of fiction: $customGenre, a rough novel or keywords $customwritesumText"}
            ]
        }
        """.trimIndent()

        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(requestBody.toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Handle request failure
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (body != null) {
                    val jsonObject = JSONObject(body)
                    val jsonArray = jsonObject.getJSONArray("choices")
                    val content = jsonArray.getJSONObject(0).getJSONObject("message").getString("content")
                    callback(content)
                }
            }
        })
    }
}