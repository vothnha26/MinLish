package com.edu.minlish.features.library.domain.model

import java.util.Date

data class VocabularySet(
    val id: String = "",
    val creatorId: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "", // e.g. IELTS, TOEIC, Communication
    val wordCount: Int = 0,
    val createdAt: Date = Date()
)
