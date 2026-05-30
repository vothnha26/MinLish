package com.edu.minlish.features.library.domain.repository

import com.edu.minlish.features.library.domain.model.VocabularyWord
import com.edu.minlish.features.library.data.DictionaryEntry

import com.edu.minlish.features.library.domain.model.Category
import com.edu.minlish.features.library.domain.model.VocabularySet

interface VocabularyRepository {
    // Vocabulary Sets
    suspend fun createSet(set: VocabularySet): Result<Unit>
    suspend fun getUserSets(userId: String): Result<List<VocabularySet>>
    suspend fun getSetById(setId: String): Result<VocabularySet>
    suspend fun updateSet(set: VocabularySet): Result<Unit>
    suspend fun deleteSet(setId: String): Result<Unit>
    
    // Categories
    suspend fun getCategories(userId: String): Result<List<Category>>
    suspend fun addCategory(category: Category): Result<Unit>
    suspend fun updateCategory(category: Category): Result<Unit>
    suspend fun deleteCategory(categoryId: String): Result<Unit>
    
    // Vocabulary Words
    suspend fun fetchWordDetails(word: String): Result<List<DictionaryEntry>>
    suspend fun addWord(word: VocabularyWord): Result<Unit>
    suspend fun updateWord(word: VocabularyWord): Result<Unit>
    suspend fun deleteWord(wordId: String): Result<Unit>
    suspend fun getWordsBySet(setId: String): Result<List<VocabularyWord>>
    suspend fun getWordById(wordId: String): Result<VocabularyWord>
}
