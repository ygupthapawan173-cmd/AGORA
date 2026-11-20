package com.agora.assistant

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

/** Reads user-selected documents locally. Content is returned to the caller and is not uploaded here. */
object DocumentReader {
    private const val MAX_CHARS = 30000

    fun displayName(context: Context, uri: Uri): String {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) return c.getString(0) ?: "document"
        }
        return uri.lastPathSegment ?: "document"
    }

    fun read(context: Context, uri: Uri): String {
        val name = displayName(context, uri)
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalArgumentException("Could not open selected file")
        val lower = name.lowercase()
        val text = when {
            lower.endsWith(".pdf") -> readPdf(context, bytes)
            lower.endsWith(".docx") -> readDocx(bytes)
            lower.endsWith(".xlsx") -> readXlsx(bytes)
            lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".csv") || lower.endsWith(".json") || lower.endsWith(".xml") -> String(bytes, Charsets.UTF_8)
            else -> throw IllegalArgumentException("Supported files: PDF, DOCX, XLSX, TXT, MD, CSV, JSON, XML")
        }
        return text.take(MAX_CHARS).ifBlank { "No readable text was found. The file may be scanned, image-only, encrypted, or empty." }
    }

    private fun readPdf(context: Context, bytes: ByteArray): String {
        PDFBoxResourceLoader.init(context.applicationContext)
        PDDocument.load(ByteArrayInputStream(bytes)).use { doc -> return PDFTextStripper().getText(doc).take(MAX_CHARS) }
    }

    private fun unzip(bytes: ByteArray): Map<String, String> {
        val result = linkedMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (!entry.isDirectory && entry.name.endsWith(".xml", true) && entry.name.length < 180) {
                    val content = zip.readBytes()
                    result[entry.name] = String(content, Charsets.UTF_8)
                }
                zip.closeEntry()
                if (result.size > 500) break
            }
        }
        return result
    }

    private fun decodeXmlText(xml: String): String = xml
        .replace(Regex("<w:tab[^>]*/>"), " ")
        .replace(Regex("<[^>]+>"), " ")
        .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
        .replace("&quot;", "\"").replace("&apos;", "'")
        .replace(Regex("\\s+"), " ").trim()

    private fun readDocx(bytes: ByteArray): String {
        val entries = unzip(bytes)
        val xml = entries["word/document.xml"] ?: throw IllegalArgumentException("DOCX document body was not found")
        return decodeXmlText(xml)
    }

    private fun readXlsx(bytes: ByteArray): String {
        val entries = unzip(bytes)
        val sharedXml = entries["xl/sharedStrings.xml"].orEmpty()
        val shared = Regex("<si(?:\\s[^>]*)?>(.*?)</si>", RegexOption.DOT_MATCHES_ALL).findAll(sharedXml)
            .map { decodeXmlText(it.groupValues[1]) }.toList()
        val sheets = entries.filterKeys { it.startsWith("xl/worksheets/sheet") && it.endsWith(".xml") }.toSortedMap()
        if (sheets.isEmpty()) throw IllegalArgumentException("No worksheet data was found")
        val out = StringBuilder()
        for ((sheetName, xml) in sheets) {
            out.append("\n").append(sheetName.substringAfterLast('/')).append("\n")
            val cells = Regex("<c(?:\\s[^>]*)?>(.*?)</c>", RegexOption.DOT_MATCHES_ALL).findAll(xml)
            for (match in cells) {
                val block = match.groupValues[1]
                val type = Regex("<c[^>]*\\bt=\"([^\"]+)\"").find(match.value)?.groupValues?.getOrNull(1)
                val value = Regex("<v>(.*?)</v>", RegexOption.DOT_MATCHES_ALL).find(block)?.groupValues?.getOrNull(1).orEmpty()
                if (value.isNotBlank()) {
                    val resolved = if (type == "s") shared.getOrNull(value.toIntOrNull() ?: -1).orEmpty() else value
                    if (resolved.isNotBlank()) out.append(resolved).append('\t')
                }
            }
            out.append('\n')
        }
        return out.toString()
    }
}
