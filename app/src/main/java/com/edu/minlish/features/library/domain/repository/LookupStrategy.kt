package com.edu.minlish.features.library.domain.repository

import com.edu.minlish.features.library.domain.model.VocabularyWord

/**
 * Strategy interface for looking up vocabulary details.
 * Follows Strategy Pattern and OCP (Open-Closed Principle).
 */
interface LookupStrategy {
    suspend fun lookupWord(word: String): Result<VocabularyWord>
}
