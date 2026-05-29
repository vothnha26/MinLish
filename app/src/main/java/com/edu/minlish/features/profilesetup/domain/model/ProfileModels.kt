package com.edu.minlish.features.profilesetup.domain.model

data class UserProfile(
    val userId: String = "",
    val learningGoal: String = "",
    val currentLevel: String = "",
    val dailyNewWordsTarget: Int = 10,
    val dailyReviewWordsTarget: Int = 20
)

data class UserLearningStats(
    val userId: String = "",
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalWordsLearned: Int = 0,
    val lastActivity: Long? = null
)
