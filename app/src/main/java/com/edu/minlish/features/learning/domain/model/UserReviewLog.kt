package com.edu.minlish.features.learning.domain.model

import java.util.Date

data class UserReviewLog(
    val id: String = "",
    val userId: String = "",
    val wordId: String = "",
    val reviewedAt: Date = Date(),
    val rating: String = "", // AGAIN, HARD, GOOD, EASY
    val intervalBefore: Int = 0,
    val intervalAfter: Int = 0
)
