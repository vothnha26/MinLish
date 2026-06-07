package com.edu.minlish.features.library.data.repository

import com.edu.minlish.core.ai.AIModule
import com.edu.minlish.core.ai.GeminiAIService
import com.edu.minlish.core.ai.model.AIAutoFillResult
import com.edu.minlish.core.util.VocabularyCache
import com.edu.minlish.features.library.domain.model.VocabularyWord
import com.edu.minlish.features.library.domain.repository.LookupStrategy
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Implementation of LookupStrategy using Gemini AI.
 */
class GeminiLookupStrategy(
    private val geminiService: GeminiAIService = AIModule.geminiService,
    private val getCachedJson: (String) -> String? = { VocabularyCache.getCachedWord(it) },
    private val cacheWordJson: (String, String) -> Unit = { w, j -> VocabularyCache.cacheWord(w, j) }
) : LookupStrategy {
    override suspend fun lookupWord(word: String): Result<VocabularyWord> = withContext(Dispatchers.IO) {
        val cleanWord = word.trim()
        if (cleanWord.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Word cannot be empty"))
        }

        // Check SharedPreferences cache first
        val cachedJson = getCachedJson(cleanWord)
        if (cachedJson != null) {
            try {
                val cachedWord = Gson().fromJson(cachedJson, VocabularyWord::class.java)
                return@withContext Result.success(cachedWord)
            } catch (e: Exception) {
                // Fallback to AI lookup if parsing fails
            }
        }

        geminiService.lookupWordDetail(cleanWord).mapCatching { jsonStr ->
            // Extract JSON if wrapped in markdown code blocks
            val cleanJson = if (jsonStr.contains("```json")) {
                jsonStr.substringAfter("```json").substringBeforeLast("```").trim()
            } else if (jsonStr.contains("```")) {
                jsonStr.substringAfter("```").substringBeforeLast("```").trim()
            } else {
                jsonStr
            }

            val autoFillResult = Gson().fromJson(cleanJson, AIAutoFillResult::class.java)
            
            val wordVal = autoFillResult.word.ifBlank { cleanWord }
            // Build VocabularyWord
            val vocabularyWord = VocabularyWord(
                word = wordVal,
                pronunciation = autoFillResult.pronunciation,
                audioUrl = "https://dict.youdao.com/dictvoice?audio=${wordVal.trim().lowercase()}&type=2",
                definitions = autoFillResult.definitions,
                collocations = autoFillResult.collocations,
                personalNote = autoFillResult.personalNote,
                imageUrl = "https://loremflickr.com/600/400/${wordVal.lowercase().trim()}?lock=${Math.abs(wordVal.hashCode())}"
            )

            // Cache the word
            try {
                val jsonWord = Gson().toJson(vocabularyWord)
                cacheWordJson(cleanWord, jsonWord)
            } catch (e: Exception) {
                // Ignore caching errors to avoid breaking the main flow
            }

            vocabularyWord
        }
    }
}
