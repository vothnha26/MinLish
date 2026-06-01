package com.edu.minlish.features.library.data.importer

import com.edu.minlish.features.library.domain.importer.VocabularyImportParser
import com.edu.minlish.features.library.domain.model.ImportVocabularyPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

class XlsxVocabularyImportParser : VocabularyImportParser {
    override suspend fun parse(
        fileName: String,
        inputStream: InputStream
    ): Result<ImportVocabularyPreview> = withContext(Dispatchers.IO) {
        try {
            val entries = readZipEntries(inputStream)
            val sheetXml = entries["xl/worksheets/sheet1.xml"]
                ?: return@withContext Result.failure(Exception("Cannot find first worksheet in XLSX file"))
            val sharedStrings = entries["xl/sharedStrings.xml"]?.let { parseSharedStrings(it) }.orEmpty()
            val records = parseSheet(sheetXml, sharedStrings)
            Result.success(ImportVocabularyRowParser.parse(fileName, records))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun readZipEntries(inputStream: InputStream): Map<String, ByteArray> {
        val result = mutableMapOf<String, ByteArray>()
        ZipInputStream(inputStream).use { zip ->
            generateSequence { zip.nextEntry }.forEach { entry ->
                if (!entry.isDirectory && (entry.name == "xl/sharedStrings.xml" || entry.name == "xl/worksheets/sheet1.xml")) {
                    result[entry.name] = zip.readBytes()
                }
                zip.closeEntry()
            }
        }
        return result
    }

    private fun parseSharedStrings(bytes: ByteArray): List<String> {
        val document = newDocument(bytes)
        val nodes = document.getElementsByTagName("si")
        return (0 until nodes.length).map { index ->
            val element = nodes.item(index) as Element
            val textNodes = element.getElementsByTagName("t")
            buildString {
                for (i in 0 until textNodes.length) {
                    append(textNodes.item(i).textContent)
                }
            }
        }
    }

    private fun parseSheet(bytes: ByteArray, sharedStrings: List<String>): List<List<String>> {
        val document = newDocument(bytes)
        val rowNodes = document.getElementsByTagName("row")
        return (0 until rowNodes.length).map { rowIndex ->
            val row = rowNodes.item(rowIndex) as Element
            val cells = row.getElementsByTagName("c")
            val values = mutableMapOf<Int, String>()

            for (cellIndex in 0 until cells.length) {
                val cell = cells.item(cellIndex) as Element
                val reference = cell.getAttribute("r")
                val columnIndex = columnIndexFromReference(reference).takeIf { it >= 0 } ?: cellIndex
                values[columnIndex] = readCellValue(cell, sharedStrings)
            }

            val maxIndex = values.keys.maxOrNull() ?: -1
            (0..maxIndex).map { values[it].orEmpty() }
        }.filter { row -> row.any { it.isNotBlank() } }
    }

    private fun readCellValue(cell: Element, sharedStrings: List<String>): String {
        val type = cell.getAttribute("t")
        if (type == "inlineStr") {
            return cell.getElementsByTagName("t").item(0)?.textContent.orEmpty()
        }

        val value = cell.getElementsByTagName("v").item(0)?.textContent.orEmpty()
        return if (type == "s") {
            sharedStrings.getOrNull(value.toIntOrNull() ?: -1).orEmpty()
        } else {
            value
        }
    }

    private fun columnIndexFromReference(reference: String): Int {
        val letters = reference.takeWhile { it.isLetter() }.uppercase()
        if (letters.isBlank()) return -1
        return letters.fold(0) { acc, char -> acc * 26 + (char - 'A' + 1) } - 1
    }

    private fun newDocument(bytes: ByteArray) =
        DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(ByteArrayInputStream(bytes))
}
