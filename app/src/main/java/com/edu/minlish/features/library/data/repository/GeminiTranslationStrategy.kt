package com.edu.minlish.features.library.data.repository

import com.edu.minlish.core.ai.AIModule
import com.edu.minlish.core.ai.GeminiAIService
import com.edu.minlish.features.library.domain.repository.TranslationStrategy

/**
 * Implementation of TranslationStrategy using Gemini AI.
 */
class GeminiTranslationStrategy(
    private val geminiService: GeminiAIService = AIModule.geminiService
) : TranslationStrategy {
    override suspend fun translate(text: String, sourceLang: String, targetLang: String): Result<String> {
        val cleanText = text.trim()
        if (cleanText.isEmpty()) {
            return Result.success("")
        }
        return geminiService.translateText(cleanText, sourceLang, targetLang)
    }
}
