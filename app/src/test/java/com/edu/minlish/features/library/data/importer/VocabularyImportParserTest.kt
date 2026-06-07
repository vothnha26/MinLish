package com.edu.minlish.features.library.data.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class VocabularyImportParserTest {

    @Test
    fun testCsvParser_withCommaDelimiter() {
        val parser = CsvVocabularyImportParser()
        val csvData = """
            Word,Pronunciation,POS,Meaning (VN),Definition (EN),Example,Synonyms,Antonyms,Collocations,Note
            abandon,/əˈbændən/,verb,từ bỏ,to leave behind,He abandoned the ship,leave;quit,keep;continue,abandon plan,personal note
        """.trimIndent()

        val inputStream = ByteArrayInputStream(csvData.toByteArray(Charsets.UTF_8))
        val result = kotlinx.coroutines.runBlocking {
            parser.parse("test.csv", inputStream)
        }

        assertTrue(result.isSuccess)
        val preview = result.getOrThrow()
        
        assertEquals(preview.errors.joinToString { it.message }, 1, preview.validRows.size)
        val row = preview.validRows[0]
        assertEquals("abandon", row.word)
        assertEquals("từ bỏ", row.meaningVietnamese)
        assertEquals("/əˈbændən/", row.pronunciation)
        assertEquals("verb", row.pos)
        assertEquals("to leave behind", row.definitionEnglish)
        assertEquals("He abandoned the ship", row.exampleSentence)
        assertEquals(listOf("leave", "quit"), row.synonyms)
        assertEquals(listOf("keep", "continue"), row.antonyms)
        assertEquals("abandon plan", row.collocations)
        assertEquals("personal note", row.personalNote)
    }

    @Test
    fun testCsvParser_withSemicolonDelimiter() {
        val parser = CsvVocabularyImportParser()
        val csvData = """
            Word;Pronunciation;POS;Meaning (VN);Definition (EN);Example;Synonyms;Antonyms;Collocations;Note
            abandon;/əˈbændən/;verb;từ bỏ;to leave behind;He abandoned the ship;leave;quit;keep;continue;abandon plan;personal note
        """.trimIndent()

        val inputStream = ByteArrayInputStream(csvData.toByteArray(Charsets.UTF_8))
        val result = kotlinx.coroutines.runBlocking {
            parser.parse("test.csv", inputStream)
        }

        assertTrue(result.isSuccess)
        val preview = result.getOrThrow()
        
        assertEquals(preview.errors.joinToString { it.message }, 1, preview.validRows.size)
        val row = preview.validRows[0]
        assertEquals("abandon", row.word)
        assertEquals("từ bỏ", row.meaningVietnamese)
    }

    @Test
    fun testXlsxParser_withValidData() {
        val parser = XlsxVocabularyImportParser()
        
        val sharedStringsXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="6" uniqueCount="6">
                <si><t>Word</t></si>
                <si><t>Meaning (VN)</t></si>
                <si><t>Pronunciation</t></si>
                <si><t>abandon</t></si>
                <si><t>từ bỏ</t></si>
                <si><t>/əˈbændən/</t></si>
            </sst>
        """.trimIndent()

        val sheet1Xml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                <sheetData>
                    <row r="1">
                        <c r="A1" t="s"><v>0</v></c>
                        <c r="B1" t="s"><v>1</v></c>
                        <c r="C1" t="s"><v>2</v></c>
                    </row>
                    <row r="2">
                        <c r="A2" t="s"><v>3</v></c>
                        <c r="B2" t="s"><v>4</v></c>
                        <c r="C2" t="s"><v>5</v></c>
                    </row>
                </sheetData>
            </worksheet>
        """.trimIndent()

        // Tạo file zip XLSX giả lập
        val outputStream = ByteArrayOutputStream()
        ZipOutputStream(outputStream).use { zip ->
            zip.putNextEntry(ZipEntry("xl/sharedStrings.xml"))
            zip.write(sharedStringsXml.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
            zip.write(sheet1Xml.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }

        val inputStream = ByteArrayInputStream(outputStream.toByteArray())
        val result = kotlinx.coroutines.runBlocking {
            parser.parse("test.xlsx", inputStream)
        }

        assertTrue(result.isSuccess)
        val preview = result.getOrThrow()
        
        assertEquals(preview.errors.joinToString { it.message }, 1, preview.validRows.size)
        val row = preview.validRows[0]
        assertEquals("abandon", row.word)
        assertEquals("từ bỏ", row.meaningVietnamese)
        assertEquals("/əˈbændən/", row.pronunciation)
    }
}
