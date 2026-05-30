package com.edu.minlish.features.library.data.repository

import com.edu.minlish.features.library.domain.repository.CollocationStrategy
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Implementation of CollocationStrategy using Datamuse API.
 */
class DatamuseCollocationStrategy(
    private val client: OkHttpClient = OkHttpClient()
) : CollocationStrategy {
    
    override suspend fun fetchCollocations(word: String): Result<List<String>> {
        val cleanWord = word.lowercase().trim()
        if (cleanWord.isBlank()) return Result.success(emptyList())
        
        return withContext(Dispatchers.IO) {
            try {
                val collocations = mutableSetOf<String>()

                // 1. Fetch adjectives that modify this noun (e.g. "ocean" -> "vast ocean")
                fetchFromUrl("https://api.datamuse.com/words?rel_jjb=$cleanWord&max=5")
                    .onSuccess { list ->
                        list.forEach { adj -> 
                            if (adj.isNotBlank()) collocations.add("$adj $cleanWord") 
                        }
                    }

                // 2. Fetch nouns modified by this adjective (e.g. "yellow" -> "yellow sun")
                fetchFromUrl("https://api.datamuse.com/words?rel_jja=$cleanWord&max=5")
                    .onSuccess { list ->
                        list.forEach { noun -> 
                            if (noun.isNotBlank()) collocations.add("$cleanWord $noun") 
                        }
                    }

                // 3. Fetch left-context (words preceding this word, e.g. "make decision")
                fetchFromUrl("https://api.datamuse.com/words?rc=$cleanWord&max=5")
                    .onSuccess { list ->
                        list.forEach { pred ->
                            if (pred.length > 2) {
                                collocations.add("$pred $cleanWord")
                            }
                        }
                    }

                // 4. Fetch right-context (words following this word, e.g. "drink water")
                fetchFromUrl("https://api.datamuse.com/words?lc=$cleanWord&max=5")
                    .onSuccess { list ->
                        list.forEach { succ ->
                            if (succ.length > 2) {
                                collocations.add("$cleanWord $succ")
                            }
                        }
                    }

                // Take top 6 collocations and return them
                Result.success(collocations.take(6).toList())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun fetchFromUrl(url: String): Result<List<String>> {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .build()
                
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.failure(Exception("HTTP error: ${response.code}"))
                }
                val bodyString = response.body?.string() ?: return Result.success(emptyList())
                val jsonArray = JSONArray(bodyString)
                val words = mutableListOf<String>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.optJSONObject(i)
                    val word = obj?.optString("word")
                    if (!word.isNullOrBlank()) {
                        words.add(word)
                    }
                }
                Result.success(words)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
