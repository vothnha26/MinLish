package com.edu.minlish.features.library.domain.repository

/**
 * Strategy interface for translating text.
 * Follows Strategy Pattern and OCP (Open-Closed Principle).
 */
interface TranslationStrategy {
    suspend fun translate(text: String, sourceLang: String = "en", targetLang: String = "vi"): Result<String>
}
