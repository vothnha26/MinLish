package com.edu.minlish.features.library.domain.repository

/**
 * Strategy interface for fetching word collocations.
 * Follows Strategy Pattern and OCP (Open-Closed Principle).
 */
interface CollocationStrategy {
    suspend fun fetchCollocations(word: String): Result<List<String>>
}
