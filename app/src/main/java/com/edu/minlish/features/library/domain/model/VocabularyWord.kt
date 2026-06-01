package com.edu.minlish.features.library.domain.model

import java.util.Date

data class WordDefinition(
    val pos: String = "",
    val meaningVietnamese: String = "",
    val definitionEnglish: String = "",
    val exampleSentence: String = "",
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList()
)

data class VocabularyWord(
    val id: String = "",
    val vocabularySetId: String = "",
    val word: String = "",
    val pronunciation: String = "",
    val audioUrl: String = "",
    val definitions: List<WordDefinition> = emptyList(),
    val collocations: String = "",
    val personalNote: String = "",
    val imageUrl: String = "",
    // Spaced repetition fields
    val interval: Int = 1,
    val repetition: Int = 0,
    val easeFactor: Double = 2.5,
    val nextReview: Long = System.currentTimeMillis(),
    val createdAt: Date = Date()
)
