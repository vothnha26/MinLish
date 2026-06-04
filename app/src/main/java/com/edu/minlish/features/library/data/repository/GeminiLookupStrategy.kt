package com.edu.minlish.features.library.data.repository

import com.edu.minlish.core.ai.AIModule
import com.edu.minlish.core.ai.GeminiAIService
import com.edu.minlish.core.ai.model.AIAutoFillResult
import com.edu.minlish.features.library.domain.model.VocabularyWord
import com.edu.minlish.features.library.domain.repository.LookupStrategy
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Implementation of LookupStrategy using Gemini AI.
 */
class GeminiLookupStrategy(
    private val geminiService: GeminiAIService = AIModule.geminiService
) : LookupStrategy {
    override suspend fun lookupWord(word: String): Result<VocabularyWord> = withContext(Dispatchers.IO) {
        val cleanWord = word.trim()
        if (cleanWord.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Word cannot be empty"))
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
            VocabularyWord(
                word = wordVal,
                pronunciation = autoFillResult.pronunciation,
                audioUrl = "https://dict.youdao.com/dictvoice?audio=${wordVal.trim().lowercase()}&type=2",
                definitions = autoFillResult.definitions,
                collocations = autoFillResult.collocations,
                personalNote = autoFillResult.personalNote,
                imageUrl = "https://loremflickr.com/600/400/${wordVal.lowercase().trim()}?lock=${Math.abs(wordVal.hashCode())}"
            )
        }
    }
}
