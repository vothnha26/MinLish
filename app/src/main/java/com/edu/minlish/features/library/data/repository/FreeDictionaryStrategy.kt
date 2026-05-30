package com.edu.minlish.features.library.data.repository

import com.edu.minlish.core.network.NetworkModule
import com.edu.minlish.features.library.data.DictionaryApiService
import com.edu.minlish.features.library.data.DictionaryEntry
import com.edu.minlish.features.library.domain.repository.DictionaryStrategy

/**
 * Implementation using api.dictionaryapi.dev (Free Dictionary API)
 */
class FreeDictionaryStrategy(
    private val apiService: DictionaryApiService = NetworkModule.createDictionaryService()
) : DictionaryStrategy {
    override suspend fun getWordDetails(word: String): Result<List<DictionaryEntry>> {
        return try {
            val response = apiService.getWordDefinition(word)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
