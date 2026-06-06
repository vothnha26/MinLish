package com.edu.minlish.features.library.data.repository

import com.edu.minlish.features.library.domain.model.VocabularyWord
import com.edu.minlish.features.library.domain.model.WordDefinition
import com.edu.minlish.features.library.domain.model.VocabularySet
import com.edu.minlish.features.library.domain.repository.VocabularyWordRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.Date

class VocabularyWordRepositoryImpl(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : VocabularyWordRepository {

    override suspend fun addWord(word: VocabularyWord): Result<Unit> {
        return try {
            withTimeout(15000) {
                firestore.collection("vocabulary_words")
                    .add(word)
                    .await()

                firestore.collection("vocabulary_sets")
                    .document(word.vocabularySetId)
                    .update("wordCount", FieldValue.increment(1))
                    .await()
            }
            Result.success(Unit)
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
                        .update("wordCount", FieldValue.increment(-1))
                        .await()
                }
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
                mapDocumentToWord(doc)
            }
            Result.success(words)
        } catch (e: Exception) {
            Result.failure(e)
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
            val word = mapDocumentToWord(doc)
            if (word != null) {
                Result.success(word)
            } else {
                Result.failure(Exception("Word not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun importWords(set: VocabularySet, words: List<VocabularyWord>): Result<Unit> {
        if (words.isEmpty()) {
            return Result.failure(Exception("No valid words to import"))
        }

        return try {
            withContext(Dispatchers.IO) {
                withTimeout(30000) {
                    val setRef = firestore.collection("vocabulary_sets").document()
                    val setId = setRef.id
                    val wordChunks = words.chunked(490)

                    wordChunks.forEachIndexed { index, chunk ->
                        val batch = firestore.batch()

                        if (index == 0) {
                            batch.set(
                                setRef,
                                set.copy(
                                    id = setId,
                                    wordCount = words.size
                                )
                            )
                        }

                        chunk.forEach { word ->
                            val wordRef = firestore.collection("vocabulary_words").document()
                            batch.set(
                                wordRef,
                                word.copy(
                                    id = wordRef.id,
                                    vocabularySetId = setId
                                )
                            )
                        }

                        batch.commit().await()
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mapDocumentToWord(doc: com.google.firebase.firestore.DocumentSnapshot): VocabularyWord? {
        return try {
            val data = doc.data ?: return null
            val definitionsData = (data["definitions"] as? List<*>)?.filterIsInstance<Map<String, Any>>() ?: emptyList()
            
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
                createdAt = (data["createdAt"] as? Timestamp)?.toDate() ?: Date()
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseList(value: Any?): List<String> {
        return when (value) {
            is List<*> -> value.filterIsInstance<String>()
            is String -> if (value.isBlank()) emptyList() else value.split(",").map { it.trim() }
            else -> emptyList()
        }
    }
}
