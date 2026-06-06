package com.edu.minlish.features.library.domain.repository

import com.edu.minlish.features.library.data.DictionaryEntry

interface DictionaryRepository {
    suspend fun fetchWordDetails(word: String): Result<List<DictionaryEntry>>
}
