package com.edu.minlish.features.library.data.repository

import com.edu.minlish.features.library.data.DictionaryEntry
import com.edu.minlish.features.library.domain.model.VocabularyWord
import com.edu.minlish.features.library.domain.model.WordDefinition
import com.edu.minlish.features.library.domain.repository.VocabularyRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.util.Date

import com.edu.minlish.features.library.domain.model.VocabularySet
import com.edu.minlish.features.library.domain.model.Category
import com.edu.minlish.features.library.domain.repository.DictionaryStrategy

class FirestoreVocabularyRepositoryImpl(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val dictionaryStrategy: DictionaryStrategy = FreeDictionaryStrategy()
) : VocabularyRepository {

    override suspend fun createSet(set: VocabularySet): Result<Unit> {
        return try {
            println("DEBUG: Preparing to create set: ${set.title} for user: ${set.creatorId}")
            withTimeout(10000) {
                firestore.collection("vocabulary_sets")
                    .add(set)
                    .await()
            }
            println("DEBUG: Set created successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            println("DEBUG: Error creating set: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun getUserSets(userId: String): Result<List<VocabularySet>> {
        return try {
            println("DEBUG: Fetching sets for user: $userId")
            val snapshot = withTimeout(10000) {
                firestore.collection("vocabulary_sets")
                    .whereEqualTo("creatorId", userId)
                    .get()
                    .await()
            }
            println("DEBUG: Found ${snapshot.size()} sets")
            val sets = snapshot.documents.mapNotNull { doc ->
                doc.toObject(VocabularySet::class.java)?.copy(id = doc.id)
            }
            Result.success(sets)
        } catch (e: Exception) {
            println("DEBUG: Error fetching sets: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun fetchWordDetails(word: String): Result<List<DictionaryEntry>> {
        return dictionaryStrategy.getWordDetails(word)
    }

    override suspend fun addWord(word: VocabularyWord): Result<Unit> {
        return try {
            withTimeout(15000) {
                firestore.collection("vocabulary_words")
                    .add(word)
                    .await()

                firestore.collection("vocabulary_sets")
                    .document(word.vocabularySetId)
                    .update("wordCount", com.google.firebase.firestore.FieldValue.increment(1))
                    .await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getWordsBySet(setId: String): Result<List<VocabularyWord>> {
        return try {
            val snapshot = withTimeout(10000) {
                firestore.collection("vocabulary_words")
                    .whereEqualTo("vocabularySetId", setId)
                    .get()
                    .await()
            }
            val words = snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    val definitionsData = data["definitions"] as? List<Map<String, Any>> ?: emptyList()
                    
                    val definitions = definitionsData.map { defMap ->
                        WordDefinition(
                            pos = defMap["pos"] as? String ?: "",
                            meaningVietnamese = defMap["meaningVietnamese"] as? String ?: "",
                            definitionEnglish = defMap["definitionEnglish"] as? String ?: "",
                            exampleSentence = defMap["exampleSentence"] as? String ?: "",
                            synonyms = parseList(defMap["synonyms"]),
                            antonyms = parseList(defMap["antonyms"])
                        )
                    }

                    VocabularyWord(
                        id = doc.id,
                        vocabularySetId = data["vocabularySetId"] as? String ?: "",
                        word = data["word"] as? String ?: "",
                        pronunciation = data["pronunciation"] as? String ?: "",
                        audioUrl = data["audioUrl"] as? String ?: "",
                        definitions = definitions,
                        collocations = data["collocations"] as? String ?: "",
                        personalNote = data["personalNote"] as? String ?: "",
                        imageUrl = data["imageUrl"] as? String ?: "",
                        createdAt = (data["createdAt"] as? com.google.firebase.Timestamp)?.toDate() ?: Date()
                    )
                } catch (e: Exception) {
                    println("DEBUG: Error mapping word ${doc.id}: ${e.message}")
                    null
                }
            }
            Result.success(words)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseList(value: Any?): List<String> {
        return when (value) {
            is List<*> -> value.filterIsInstance<String>()
            is String -> if (value.isBlank()) emptyList() else value.split(",").map { it.trim() }
            else -> emptyList()
        }
    }

    override suspend fun getWordById(wordId: String): Result<VocabularyWord> {
        return try {
            val doc = withTimeout(10000) {
                firestore.collection("vocabulary_words")
                    .document(wordId)
                    .get()
                    .await()
            }
            val data = doc.data
            if (data != null) {
                val definitionsData = data["definitions"] as? List<Map<String, Any>> ?: emptyList()
                val definitions = definitionsData.map { defMap ->
                    WordDefinition(
                        pos = defMap["pos"] as? String ?: "",
                        meaningVietnamese = defMap["meaningVietnamese"] as? String ?: "",
                        definitionEnglish = defMap["definitionEnglish"] as? String ?: "",
                        exampleSentence = defMap["exampleSentence"] as? String ?: "",
                        synonyms = parseList(defMap["synonyms"]),
                        antonyms = parseList(defMap["antonyms"])
                    )
                }
                val word = VocabularyWord(
                    id = doc.id,
                    vocabularySetId = data["vocabularySetId"] as? String ?: "",
                    word = data["word"] as? String ?: "",
                    pronunciation = data["pronunciation"] as? String ?: "",
                    audioUrl = data["audioUrl"] as? String ?: "",
                    definitions = definitions,
                    collocations = data["collocations"] as? String ?: "",
                    personalNote = data["personalNote"] as? String ?: "",
                    imageUrl = data["imageUrl"] as? String ?: "",
                    createdAt = (data["createdAt"] as? com.google.firebase.Timestamp)?.toDate() ?: Date()
                )
                Result.success(word)
            } else {
                Result.failure(Exception("Word not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateWord(word: VocabularyWord): Result<Unit> {
        return try {
            withTimeout(10000) {
                firestore.collection("vocabulary_words")
                    .document(word.id)
                    .set(word)
                    .await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteWord(wordId: String): Result<Unit> {
        return try {
            withTimeout(20000) {
                val doc = firestore.collection("vocabulary_words")
                    .document(wordId)
                    .get()
                    .await()
                val setId = doc.getString("vocabularySetId")

                firestore.collection("vocabulary_words")
                    .document(wordId)
                    .delete()
                    .await()

                if (setId != null) {
                    firestore.collection("vocabulary_sets")
                        .document(setId)
                        .update("wordCount", com.google.firebase.firestore.FieldValue.increment(-1))
                        .await()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getSetById(setId: String): Result<VocabularySet> {
        return try {
            val doc = withTimeout(10000) {
                firestore.collection("vocabulary_sets")
                    .document(setId)
                    .get()
                    .await()
            }
            val set = doc.toObject(VocabularySet::class.java)?.copy(id = doc.id)
            if (set != null) {
                Result.success(set)
            } else {
                Result.failure(Exception("Set not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateSet(set: VocabularySet): Result<Unit> {
        return try {
            withTimeout(10000) {
                firestore.collection("vocabulary_sets")
                    .document(set.id)
                    .set(set)
                    .await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteSet(setId: String): Result<Unit> {
        return try {
            withTimeout(15000) {
                firestore.collection("vocabulary_sets")
                    .document(setId)
                    .delete()
                    .await()

                val wordsSnapshot = firestore.collection("vocabulary_words")
                    .whereEqualTo("vocabularySetId", setId)
                    .get()
                    .await()
                
                for (doc in wordsSnapshot.documents) {
                    firestore.collection("vocabulary_words")
                        .document(doc.id)
                        .delete()
                        .await()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCategories(userId: String): Result<List<Category>> {
        return try {
            val snapshot = withTimeout(10000) {
                firestore.collection("categories")
                    .whereEqualTo("creatorId", userId)
                    .get()
                    .await()
            }
            val categories = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Category::class.java)?.copy(id = doc.id)
            }
            Result.success(categories)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addCategory(category: Category): Result<Unit> {
        return try {
            withTimeout(10000) {
                firestore.collection("categories")
                    .add(category)
                    .await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateCategory(category: Category): Result<Unit> {
        return try {
            withTimeout(10000) {
                firestore.collection("categories")
                    .document(category.id)
                    .set(category)
                    .await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteCategory(categoryId: String): Result<Unit> {
        return try {
            withTimeout(10000) {
                firestore.collection("categories")
                    .document(categoryId)
                    .delete()
                    .await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
