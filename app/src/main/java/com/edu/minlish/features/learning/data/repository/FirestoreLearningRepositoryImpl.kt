package com.edu.minlish.features.learning.data.repository

import com.edu.minlish.features.learning.domain.model.UserReviewLog
import com.edu.minlish.features.learning.domain.model.UserWordProgress
import com.edu.minlish.features.learning.domain.repository.LearningRepository
import com.edu.minlish.features.library.domain.model.VocabularyWord
import com.edu.minlish.features.library.domain.model.WordDefinition
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.util.*

class FirestoreLearningRepositoryImpl(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : LearningRepository {

    override suspend fun getDueWords(userId: String, setId: String?, forceAll: Boolean): Result<List<Pair<VocabularyWord, UserWordProgress?>>> {
        return try {
            withTimeout(15000) {
                // 1. Get all words in the set (or all words if setId is null)
                val wordsQuery = if (setId != null) {
                    firestore.collection("vocabulary_words").whereEqualTo("vocabularySetId", setId)
                } else {
                    firestore.collection("vocabulary_words")
                }
                val wordsSnapshot = wordsQuery.get().await()
                
                // Manual mapping to handle backward compatibility (String vs List)
                val words = wordsSnapshot.documents.mapNotNull { doc ->
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
                            imageUrl = data["imageUrl"] as? String ?: ""
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

                // 2. Get user progress for these words
                val progressQuery = firestore.collection("user_word_progress")
                    .whereEqualTo("userId", userId)
                
                val progressSnapshot = progressQuery.get().await()
                val progressMap = progressSnapshot.documents.associate { doc ->
                    val p = doc.toObject(UserWordProgress::class.java)!!
                    p.wordId to p.copy(id = doc.id)
                }

                // 3. Filter due words (Progress is null OR nextReviewDate <= now)
                val now = Date()
                val dueWords = words.map { word ->
                    word to progressMap[word.id]
                }.filter { (_, progress) ->
                    forceAll || progress == null || progress.nextReviewDate.before(now) || progress.nextReviewDate == now
                }

                Result.success(dueWords)
            }
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

    override suspend fun updateProgress(progress: UserWordProgress): Result<Unit> {
        return try {
            withTimeout(10000) {
                if (progress.id.isNotEmpty()) {
                    firestore.collection("user_word_progress").document(progress.id)
                        .set(progress, SetOptions.merge())
                        .await()
                } else {
                    firestore.collection("user_word_progress")
                        .add(progress)
                        .await()
                    // Note: Here we'd ideally update the ID in the object, but for simplicity returning Unit
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun initializeProgress(userId: String, wordId: String, setId: String): Result<UserWordProgress> {
        val newProgress = UserWordProgress(
            userId = userId,
            wordId = wordId,
            setId = setId
        )
        return Result.success(newProgress)
    }

    override suspend fun getDailySessionWords(
        userId: String,
        targetNew: Int,
        targetReview: Int,
        setId: String?
    ): Result<List<Pair<VocabularyWord, UserWordProgress?>>> {
        return try {
            withTimeout(20000) {
                // 1. Get all words in set (or all words if setId is null)
                val wordsQuery = if (setId != null) {
                    firestore.collection("vocabulary_words").whereEqualTo("vocabularySetId", setId)
                } else {
                    firestore.collection("vocabulary_words")
                }
                val wordsSnapshot = wordsQuery.get().await()
                
                val words = wordsSnapshot.documents.mapNotNull { doc ->
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
                            imageUrl = data["imageUrl"] as? String ?: ""
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

                // 2. Get user progress for these words
                val progressSnapshot = firestore.collection("user_word_progress")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()
                
                val progressMap = progressSnapshot.documents.associate { doc ->
                    val p = doc.toObject(UserWordProgress::class.java)!!
                    p.wordId to p.copy(id = doc.id)
                }

                // 3. Classify words into Due (Review) and New
                val now = Date()
                val classified = words.map { word ->
                    word to progressMap[word.id]
                }

                val dueWords = classified.filter { (_, progress) ->
                    progress != null && (progress.nextReviewDate.before(now) || progress.nextReviewDate == now)
                }

                val newWords = classified.filter { (_, progress) ->
                    progress == null
                }

                // 4. Limit based on target
                val selectedDue = dueWords.take(targetReview)
                val selectedNew = newWords.take(targetNew)

                Result.success(selectedDue + selectedNew)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logReview(log: UserReviewLog): Result<Unit> {
        return try {
            withTimeout(10000) {
                firestore.collection("user_review_logs")
                    .add(log)
                    .await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getReviewLogsForDate(userId: String, date: Date): Result<List<UserReviewLog>> {
        return try {
            withTimeout(15000) {
                // Get start and end of the day
                val cal = Calendar.getInstance().apply {
                    time = date
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val startOfDay = cal.time
                cal.add(Calendar.DAY_OF_YEAR, 1)
                val endOfDay = cal.time

                val snapshot = firestore.collection("user_review_logs")
                    .whereEqualTo("userId", userId)
                    .whereGreaterThanOrEqualTo("reviewedAt", startOfDay)
                    .whereLessThan("reviewedAt", endOfDay)
                    .get()
                    .await()

                val logs = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(UserReviewLog::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                }
                Result.success(logs)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
