package com.edu.minlish.features.library.data.importer

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.edu.minlish.features.library.domain.model.ImportVocabularyPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VocabularyImportManager(
    private val csvParser: CsvVocabularyImportParser = CsvVocabularyImportParser(),
    private val xlsxParser: XlsxVocabularyImportParser = XlsxVocabularyImportParser()
) {
    suspend fun parseFromUri(context: Context, uri: Uri): Result<ImportVocabularyPreview> =
        withContext(Dispatchers.IO) {
            val fileName = resolveFileName(context, uri)
            val parser = when {
                fileName.endsWith(".csv", ignoreCase = true) -> csvParser
                fileName.endsWith(".xlsx", ignoreCase = true) -> xlsxParser
                else -> return@withContext Result.failure(Exception("Only CSV and XLSX files are supported"))
            }

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                parser.parse(fileName, inputStream)
            } ?: Result.failure(Exception("Cannot open import file"))
        }

    private fun resolveFileName(context: Context, uri: Uri): String {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                return cursor.getString(nameIndex)
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "import.csv"
    }
}
