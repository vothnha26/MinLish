package com.edu.minlish.features.learning.domain.model

import java.util.Date

data class UserWordProgress(
    val id: String = "",
    val userId: String = "",
    val wordId: String = "",
    val setId: String = "",
    
    // SM-2 Parameters
    val easeFactor: Float = 2.5f,
    val interval: Int = 0,
    val repetitions: Int = 0,
    
    val nextReviewDate: Date = Date(),
    val lastReviewedAt: Date = Date(),
    val status: String = "learning" // learning, reviewing, mastered
)
