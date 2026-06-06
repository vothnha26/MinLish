package com.edu.minlish.features.library.domain.usecase

import com.edu.minlish.features.library.domain.model.ImportVocabularyPreview
import com.edu.minlish.features.library.domain.model.VocabularySet
import com.edu.minlish.features.library.domain.model.VocabularyWord
import com.edu.minlish.features.library.domain.model.WordDefinition
import com.edu.minlish.features.library.domain.repository.VocabularyWordRepository
import java.util.Date

class ImportVocabularyUseCase(
    private val wordRepository: VocabularyWordRepository
) {
    suspend operator fun invoke(
        userId: String,
        title: String,
        category: String,
        preview: ImportVocabularyPreview
    ): Result<Int> {
        return try {
            val importedAt = Date()
            val set = VocabularySet(
                creatorId = userId,
                title = title,
                description = "Imported from ${preview.fileName}",
                category = category,
                wordCount = preview.validRows.size,
                createdAt = importedAt
            )
            
            val words = preview.validRows.map { row ->
                VocabularyWord(
                    word = row.word,
                    pronunciation = row.pronunciation,
                    definitions = listOf(
                        WordDefinition(
                            pos = row.pos,
                            meaningVietnamese = row.meaningVietnamese,
                            definitionEnglish = row.definitionEnglish,
                            exampleSentence = row.exampleSentence,
                            synonyms = row.synonyms,
                            antonyms = row.antonyms
                        )
                    ),
                    collocations = row.collocations,
                    personalNote = row.personalNote,
                    createdAt = importedAt
                )
            }

            wordRepository.importWords(set, words).map { words.size }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
