package com.edu.minlish.features.library.domain.repository

import com.edu.minlish.features.library.data.DictionaryEntry

/**
 * Strategy interface for fetching dictionary data.
 * Follows Strategy Pattern and OCP (Open-Closed Principle).
 */
interface DictionaryStrategy {
    suspend fun getWordDetails(word: String): Result<List<DictionaryEntry>>
}
