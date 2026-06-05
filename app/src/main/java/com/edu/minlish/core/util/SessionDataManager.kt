package com.edu.minlish.core.util

import android.util.Log
import com.edu.minlish.features.profilesetup.domain.model.UserProfile
import com.edu.minlish.features.library.domain.model.VocabularySet
import com.edu.minlish.features.learning.domain.model.UserWordProgress
import com.edu.minlish.features.learning.domain.model.UserReviewLog
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

object SessionDataManager {
    private const val TAG = "SessionDataManager"

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfileFlow = _userProfile.asStateFlow()

    private val _vocabularySets = MutableStateFlow<List<VocabularySet>?>(null)
    val vocabularySetsFlow = _vocabularySets.asStateFlow()

    private val _userWordProgresses = MutableStateFlow<List<UserWordProgress>?>(null)
    val userWordProgressesFlow = _userWordProgresses.asStateFlow()

    private val _userReviewLogs = MutableStateFlow<List<UserReviewLog>?>(null)
    val userReviewLogsFlow = _userReviewLogs.asStateFlow()

    var userProfile: UserProfile?
        get() = _userProfile.value
        set(value) {
            _userProfile.value = value
        }

    var vocabularySets: List<VocabularySet>?
        get() = _vocabularySets.value
        set(value) {
            _vocabularySets.value = value
        }

    var userWordProgresses: List<UserWordProgress>?
        get() = _userWordProgresses.value
        set(value) {
            _userWordProgresses.value = value
        }

    var userReviewLogs: List<UserReviewLog>?
        get() = _userReviewLogs.value
        set(value) {
            _userReviewLogs.value = value
        }

    var isDataLoaded = false
        private set

    // Firebase Listener Registrations
    private var profileListener: ListenerRegistration? = null
    private var setsListener: ListenerRegistration? = null
    private var progressListener: ListenerRegistration? = null
    private var logsListener: ListenerRegistration? = null

    fun clear() {
        // Hủy bỏ các listener
        profileListener?.remove()
        profileListener = null
        setsListener?.remove()
        setsListener = null
        progressListener?.remove()
        progressListener = null
        logsListener?.remove()
        logsListener = null

        // Clear values
        _userProfile.value = null
        _vocabularySets.value = null
        _userWordProgresses.value = null
        _userReviewLogs.value = null
        isDataLoaded = false
        Log.d(TAG, "Session data cleared and listeners removed.")
    }

    suspend fun preFetchUserData(userId: String) = coroutineScope {
        if (userId.isBlank()) return@coroutineScope
        Log.d(TAG, "Starting pre-fetch and registering listeners for userId: $userId")
        
        // Hủy bỏ các listener cũ nếu có
        profileListener?.remove()
        setsListener?.remove()
        progressListener?.remove()
        logsListener?.remove()

        val firestore = FirebaseFirestore.getInstance()

        // 1. Tải một lần ban đầu (để đảm bảo isDataLoaded là true ngay lập tức và UI không bị block)
        val profileDeferred = async {
            try {
                val snapshot = firestore.collection("profiles")
                    .document(userId)
                    .get()
                    .await()
                snapshot.toObject(UserProfile::class.java)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to pre-fetch user profile: ${e.message}")
                null
            }
        }

        val setsDeferred = async {
            try {
                val snapshot = firestore.collection("vocabulary_sets")
                    .whereEqualTo("creatorId", userId)
                    .get()
                    .await()
                snapshot.toObjects(VocabularySet::class.java)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to pre-fetch vocabulary sets: ${e.message}")
                null
            }
        }

        val progressDeferred = async {
            try {
                val snapshot = firestore.collection("user_word_progress")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(UserWordProgress::class.java)?.copy(id = doc.id)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to pre-fetch word progress: ${e.message}")
                null
            }
        }

        val logsDeferred = async {
            try {
                val snapshot = firestore.collection("user_review_logs")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()
                snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(UserReviewLog::class.java)?.copy(id = doc.id)
                    } catch (ex: Exception) {
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to pre-fetch review logs: ${e.message}")
                null
            }
        }

        userProfile = profileDeferred.await()
        vocabularySets = setsDeferred.await()
        userWordProgresses = progressDeferred.await()
        userReviewLogs = logsDeferred.await()
        isDataLoaded = true

        Log.d(TAG, "Initial pre-fetch completed. Registering real-time listeners...")

        // 2. Đăng ký SnapshotListener toàn cục để tự động đồng bộ thời gian thực
        profileListener = firestore.collection("profiles")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to profile changes", error)
                    return@addSnapshotListener
                }
                snapshot?.toObject(UserProfile::class.java)?.let {
                    userProfile = it
                    Log.d(TAG, "Real-time user profile updated.")
                }
            }

        setsListener = firestore.collection("vocabulary_sets")
            .whereEqualTo("creatorId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to vocabulary sets changes", error)
                    return@addSnapshotListener
                }
                snapshot?.toObjects(VocabularySet::class.java)?.let {
                    vocabularySets = it
                    Log.d(TAG, "Real-time vocabulary sets updated: ${it.size} sets.")
                }
            }

        progressListener = firestore.collection("user_word_progress")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to word progress changes", error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(UserWordProgress::class.java)?.copy(id = doc.id)
                }
                list?.let {
                    userWordProgresses = it
                    Log.d(TAG, "Real-time word progresses updated: ${it.size} items.")
                }
            }

        logsListener = firestore.collection("user_review_logs")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to review logs changes", error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(UserReviewLog::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                }
                list?.let {
                    userReviewLogs = it
                    Log.d(TAG, "Real-time review logs updated: ${it.size} logs.")
                }
            }
    }
}
