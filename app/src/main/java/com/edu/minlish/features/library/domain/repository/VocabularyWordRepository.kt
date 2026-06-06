package com.edu.minlish.features.library.domain.repository

import com.edu.minlish.features.library.domain.model.VocabularyWord
import com.edu.minlish.features.library.domain.model.VocabularySet

interface VocabularyWordRepository {
    suspend fun addWord(word: VocabularyWord): Result<Unit>
    suspend fun updateWord(word: VocabularyWord): Result<Unit>
    suspend fun deleteWord(wordId: String): Result<Unit>
    suspend fun getWordsBySet(setId: String): Result<List<VocabularyWord>>
    suspend fun getWordById(wordId: String): Result<VocabularyWord>
    suspend fun importWords(set: VocabularySet, words: List<VocabularyWord>): Result<Unit>
}
