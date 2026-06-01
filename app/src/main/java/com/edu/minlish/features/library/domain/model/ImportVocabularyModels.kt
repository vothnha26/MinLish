package com.edu.minlish.features.library.domain.model

data class ImportVocabularyRow(
    val word: String,
    val pronunciation: String = "",
    val meaningVietnamese: String,
    val definitionEnglish: String = "",
    val exampleSentence: String = "",
    val pos: String = "",
    val collocations: String = "",
    val personalNote: String = "",
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
    val rowNumber: Int
)

data class ImportVocabularyPreview(
    val fileName: String,
    val validRows: List<ImportVocabularyRow>,
    val errors: List<ImportVocabularyError>,
    val totalRows: Int
)

data class ImportVocabularyError(
    val rowNumber: Int,
    val message: String
)
