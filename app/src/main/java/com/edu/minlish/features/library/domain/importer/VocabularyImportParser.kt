package com.edu.minlish.features.library.domain.importer

import com.edu.minlish.features.library.domain.model.ImportVocabularyPreview
import java.io.InputStream

interface VocabularyImportParser {
    suspend fun parse(
        fileName: String,
        inputStream: InputStream
    ): Result<ImportVocabularyPreview>
}
