package com.edu.minlish.features.library.data.repository

import com.edu.minlish.features.library.domain.repository.TranslationStrategy
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder

/**
 * Implementation of TranslationStrategy using Google Translate free endpoint.
 */
class GoogleTranslationStrategy(
    private val client: OkHttpClient = OkHttpClient()
) : TranslationStrategy {
    override suspend fun translate(text: String, sourceLang: String, targetLang: String): Result<String> {
        if (text.isBlank()) return Result.success("")
        
        return withContext(Dispatchers.IO) {
            try {
                val encodedText = URLEncoder.encode(text, "UTF-8")
                val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=$sourceLang&tl=$targetLang&dt=t&q=$encodedText"
                
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0")
                    .build()
                
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(Exception("Translation request failed: ${response.code}"))
                    }
                    val bodyString = response.body?.string() ?: return@withContext Result.failure(Exception("Empty translation response"))
                    
                    // Parse Google Translate single response array:
                    // [[["translatedText","originalText",...]]]
                    val jsonArray = JSONArray(bodyString)
                    val firstArray = jsonArray.optJSONArray(0)
                    if (firstArray != null && firstArray.length() > 0) {
                        val stringBuilder = StringBuilder()
                        for (i in 0 until firstArray.length()) {
                            val innerArray = firstArray.optJSONArray(i)
                            val translation = innerArray?.optString(0)
                            if (translation != null && translation != "null") {
                                stringBuilder.append(translation)
                            }
                        }
                        Result.success(stringBuilder.toString())
                    } else {
                        Result.failure(Exception("Invalid translation JSON structure"))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
