package com.edu.minlish.features.library.data.repository

import com.edu.minlish.features.library.data.DictionaryEntry
import com.edu.minlish.features.library.domain.repository.DictionaryRepository
import com.edu.minlish.features.library.domain.repository.DictionaryStrategy

class DictionaryRepositoryImpl(
    private val dictionaryStrategy: DictionaryStrategy = FreeDictionaryStrategy()
) : DictionaryRepository {

    override suspend fun fetchWordDetails(word: String): Result<List<DictionaryEntry>> {
        return dictionaryStrategy.getWordDetails(word)
    }
}
