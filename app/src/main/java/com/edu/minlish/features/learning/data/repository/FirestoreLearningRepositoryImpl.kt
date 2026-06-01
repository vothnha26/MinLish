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

    // ─── Public API ────────────────────────────────────────────────────────────

    override suspend fun getDueWords(
        userId: String,
        setId: String?,
        forceAll: Boolean
    ): Result<List<Pair<VocabularyWord, UserWordProgress?>>> {
        return try {
            withTimeout(15000) {
                val words = fetchUserWords(userId, setId)
                val progressMap = fetchProgressMap(userId)
                val now = Date()

                val dueWords = words.map { word ->
                    word to progressMap[word.id]
                }.filter { (_, progress) ->
                    forceAll ||
                    progress == null ||
                    progress.nextReviewDate.before(now) ||
                    progress.nextReviewDate == now
                }

                Result.success(dueWords)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getDailySessionWords(
        userId: String,
        targetNew: Int,
        targetReview: Int,
        setId: String?
    ): Result<List<Pair<VocabularyWord, UserWordProgress?>>> {
        return try {
            withTimeout(20000) {
                val words = fetchUserWords(userId, setId)
                val progressMap = fetchProgressMap(userId)
                val now = Date()

                val classified = words.map { word -> word to progressMap[word.id] }

                val dueWords = classified
                    .filter { (_, p) -> p != null && (p.nextReviewDate.before(now) || p.nextReviewDate == now) }
                    .take(targetReview)

                val newWords = classified
                    .filter { (_, p) -> p == null }
                    .take(targetNew)

                Result.success(dueWords + newWords)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProgress(progress: UserWordProgress): Result<Unit> {
        return try {
            withTimeout(10000) {
                if (progress.id.isNotEmpty()) {
                    firestore.collection("user_word_progress")
                        .document(progress.id)
                        .set(progress, SetOptions.merge())
                        .await()
                } else {
                    firestore.collection("user_word_progress")
                        .add(progress)
                        .await()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun initializeProgress(
        userId: String,
        wordId: String,
        setId: String
    ): Result<UserWordProgress> {
        val newProgress = UserWordProgress(
            userId = userId,
            wordId = wordId,
            setId = setId
        )
        return Result.success(newProgress)
    }

    override suspend fun logReview(log: UserReviewLog): Result<Unit> {
        return try {
            withTimeout(10000) {
                firestore.collection("user_review_logs").add(log).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getReviewLogsForDate(
        userId: String,
        date: Date
    ): Result<List<UserReviewLog>> {
        return try {
            withTimeout(15000) {
                val cal = Calendar.getInstance().apply {
                    time = date
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                val startOfDay = cal.time
                cal.add(Calendar.DAY_OF_YEAR, 1)
                val endOfDay = cal.time

                val snapshot = firestore.collection("user_review_logs")
                    .whereEqualTo("userId", userId)
                    .whereGreaterThanOrEqualTo("reviewedAt", startOfDay)
                    .whereLessThan("reviewedAt", endOfDay)
                    .get().await()

                val logs = snapshot.documents.mapNotNull { doc ->
                    try { doc.toObject(UserReviewLog::class.java)?.copy(id = doc.id) }
                    catch (e: Exception) { null }
                }
                Result.success(logs)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── Private helpers ───────────────────────────────────────────────────────

    /**
     * Lấy words thuộc về user:
     * - Nếu có [setId]: verify set đó thuộc [userId], rồi lấy words trong set.
     * - Nếu không có [setId]: lấy tất cả sets của [userId], rồi lấy words trong đó.
     */
    private suspend fun fetchUserWords(userId: String, setId: String?): List<VocabularyWord> {
        return if (setId != null) {
            // Verify set này thuộc về user
            val setDoc = firestore.collection("vocabulary_sets")
                .document(setId)
                .get().await()
            val creatorId = setDoc.getString("creatorId")
            if (creatorId != userId) {
                // Set không thuộc về user này — trả về empty
                return emptyList()
            }
            fetchWordsInSet(setId)
        } else {
            // Lấy tất cả sets của user
            val setsSnapshot = firestore.collection("vocabulary_sets")
                .whereEqualTo("creatorId", userId)
                .get().await()

            val userSetIds = setsSnapshot.documents.map { it.id }
            if (userSetIds.isEmpty()) return emptyList()

            // Lấy words trong tất cả sets của user (Firestore giới hạn whereIn 10 items)
            userSetIds
                .chunked(10)
                .flatMap { chunk ->
                    firestore.collection("vocabulary_words")
                        .whereIn("vocabularySetId", chunk)
                        .get().await()
                        .documents
                        .mapNotNull { doc -> mapWord(doc) }
                }
        }
    }

    private suspend fun fetchWordsInSet(setId: String): List<VocabularyWord> {
        val snapshot = firestore.collection("vocabulary_words")
            .whereEqualTo("vocabularySetId", setId)
            .get().await()
        return snapshot.documents.mapNotNull { doc -> mapWord(doc) }
    }

    private suspend fun fetchProgressMap(userId: String): Map<String, UserWordProgress> {
        val snapshot = firestore.collection("user_word_progress")
            .whereEqualTo("userId", userId)
            .get().await()
        return snapshot.documents.associate { doc ->
            val p = doc.toObject(UserWordProgress::class.java)!!
            p.wordId to p.copy(id = doc.id)
        }
    }

    private fun mapWord(doc: com.google.firebase.firestore.DocumentSnapshot): VocabularyWord? {
        return try {
            val data = doc.data ?: return null
            val definitionsData = (data["definitions"] as? List<*>)
                ?.filterIsInstance<Map<String, Any>>() ?: emptyList()

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

    private fun parseList(value: Any?): List<String> = when (value) {
        is List<*> -> value.filterIsInstance<String>()
        is String -> if (value.isBlank()) emptyList() else value.split(",").map { it.trim() }
        else -> emptyList()
    }
}
