package com.edu.minlish.features.library.data.importer

import com.edu.minlish.features.library.domain.model.ImportVocabularyError
import com.edu.minlish.features.library.domain.model.ImportVocabularyPreview
import com.edu.minlish.features.library.domain.model.ImportVocabularyRow
import java.util.Locale

internal object ImportVocabularyRowParser {
    private const val MAX_IMPORT_ROWS = 490

    private val headerAliases = mapOf(
        "word" to "word",
        "english" to "word",
        "term" to "word",
        "vocab" to "word",
        "vocabulary" to "word",
        "pronunciation" to "pronunciation",
        "ipa" to "pronunciation",
        "phonetic" to "pronunciation",
        "meaningvietnamese" to "meaningVietnamese",
        "meaning" to "meaningVietnamese",
        "vietnamese" to "meaningVietnamese",
        "translation" to "meaningVietnamese",
        "definitionenglish" to "definitionEnglish",
        "definition" to "definitionEnglish",
        "englishdefinition" to "definitionEnglish",
        "examplesentence" to "exampleSentence",
        "example" to "exampleSentence",
        "sentence" to "exampleSentence",
        "pos" to "pos",
        "partofspeech" to "pos",
        "type" to "pos",
        "collocations" to "collocations",
        "collocation" to "collocations",
        "personalnote" to "personalNote",
        "note" to "personalNote",
        "synonyms" to "synonyms",
        "synonym" to "synonyms",
        "antonyms" to "antonyms",
        "antonym" to "antonyms"
    )

    fun parse(fileName: String, records: List<List<String>>): ImportVocabularyPreview {
        if (records.isEmpty()) {
            return ImportVocabularyPreview(
                fileName = fileName,
                validRows = emptyList(),
                errors = listOf(ImportVocabularyError(1, "File is empty")),
                totalRows = 0
            )
        }

        val header = records.first().map { normalizeHeader(it) }
        val headerIndex = buildHeaderIndex(header)
        val errors = mutableListOf<ImportVocabularyError>()
        val validRows = mutableListOf<ImportVocabularyRow>()
        val seenWords = mutableSetOf<String>()

        if (!headerIndex.containsKey("word")) {
            errors.add(ImportVocabularyError(1, "Missing required column: word"))
        }
        if (!headerIndex.containsKey("meaningVietnamese")) {
            errors.add(ImportVocabularyError(1, "Missing required column: meaningVietnamese"))
        }
        if (errors.isNotEmpty()) {
            return ImportVocabularyPreview(
                fileName = fileName,
                validRows = emptyList(),
                errors = errors,
                totalRows = (records.size - 1).coerceAtLeast(0)
            )
        }

        records.drop(1).forEachIndexed { index, record ->
            val rowNumber = index + 2
            if (record.all { it.isBlank() }) return@forEachIndexed

            if (validRows.size >= MAX_IMPORT_ROWS) {
                errors.add(ImportVocabularyError(rowNumber, "Import limit is $MAX_IMPORT_ROWS words per file"))
                return@forEachIndexed
            }

            val word = record.valueAt(headerIndex, "word")
            val meaningVietnamese = record.valueAt(headerIndex, "meaningVietnamese")

            when {
                word.isBlank() -> {
                    errors.add(ImportVocabularyError(rowNumber, "Missing word"))
                    return@forEachIndexed
                }
                meaningVietnamese.isBlank() -> {
                    errors.add(ImportVocabularyError(rowNumber, "Missing meaningVietnamese"))
                    return@forEachIndexed
                }
                word.length > 100 -> {
                    errors.add(ImportVocabularyError(rowNumber, "Word is longer than 100 characters"))
                    return@forEachIndexed
                }
                meaningVietnamese.length > 500 -> {
                    errors.add(ImportVocabularyError(rowNumber, "meaningVietnamese is longer than 500 characters"))
                    return@forEachIndexed
                }
            }

            val wordKey = word.lowercase(Locale.US)
            if (!seenWords.add(wordKey)) {
                errors.add(ImportVocabularyError(rowNumber, "Duplicate word in file: $word"))
                return@forEachIndexed
            }

            validRows.add(
                ImportVocabularyRow(
                    word = word,
                    pronunciation = record.valueAt(headerIndex, "pronunciation"),
                    meaningVietnamese = meaningVietnamese,
                    definitionEnglish = record.valueAt(headerIndex, "definitionEnglish"),
                    exampleSentence = record.valueAt(headerIndex, "exampleSentence"),
                    pos = record.valueAt(headerIndex, "pos"),
                    collocations = record.valueAt(headerIndex, "collocations"),
                    personalNote = record.valueAt(headerIndex, "personalNote"),
                    synonyms = parseList(record.valueAt(headerIndex, "synonyms")),
                    antonyms = parseList(record.valueAt(headerIndex, "antonyms")),
                    rowNumber = rowNumber
                )
            )
        }

        return ImportVocabularyPreview(
            fileName = fileName,
            validRows = validRows,
            errors = errors,
            totalRows = (records.size - 1).coerceAtLeast(0)
        )
    }

    private fun buildHeaderIndex(headers: List<String>): Map<String, Int> {
        return headers.mapIndexedNotNull { index, rawHeader ->
            val canonical = headerAliases[rawHeader] ?: rawHeader.takeIf { it in headerAliases.values }
            canonical?.let { it to index }
        }.toMap()
    }

    private fun normalizeHeader(value: String): String {
        return value
            .trim()
            .removePrefix("\uFEFF")
            .replace("_", "")
            .replace(" ", "")
            .replace("-", "")
            .lowercase(Locale.US)
    }

    private fun List<String>.valueAt(headerIndex: Map<String, Int>, column: String): String {
        val index = headerIndex[column] ?: return ""
        return getOrNull(index)?.trim().orEmpty()
    }

    private fun parseList(value: String): List<String> {
        return value
            .split(";")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }
}
