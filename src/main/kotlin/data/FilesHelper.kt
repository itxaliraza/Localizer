package data

import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory


object FilesHelper {


    fun getFilesXmlContents(fileContents: Map<String, String>): Map<String, FileXmlData> {
        return fileContents.mapValues { (fileName, content) ->
            val currentPairs = parseXml(content)
            FileXmlData(
                contents = content,
                keyValuePairs = currentPairs,
                languageCode = extractLanguageCode(fileName).second
            )
        }
    }

    fun extractLanguageCode(fileName: String): Pair<String, String> {
        // Allow `+` so BCP47 qualifier folders (values-b+ms+Arab) are captured too.
        val regex = Regex("""values-([\w+-]+)/""")
        val rawCode = regex.find(fileName)?.groups?.get(1)?.value ?: return "en" to "en"

        // First undo any Android resource qualifier form (pt-rBR -> pt-BR, b+ms+Arab -> ms-Arab),
        // then apply the legacy / Google-Translate remaps.
        val localeCode = fromAndroidResFolderCode(rawCode)
        val remapped = when (localeCode) {
            "zh", "zh-CN" -> "zh-CN"
            "zh-TW" -> "zh-TW"
            "in" -> "id"
            "he" -> "iw"
            "ji" -> "yi"
            else -> localeCode
        }

        return rawCode to resolveAvailableCode(remapped)
    }

    /**
     * Resolves a locale code to the exact code present in [availableLanguages]. Tries a
     * case-insensitive exact match first; if none, falls back to the base language subtag — so an
     * Android region folder like `values-pt-rBR` (→ `pt-BR`) still resolves to the available `pt`
     * entry, and `values-es-rMX` resolves to `es`. Returns the input unchanged if nothing matches.
     */
    private fun resolveAvailableCode(code: String): String {
        availableLanguages.firstOrNull { it.langCode.equals(code, ignoreCase = true) }?.let { return it.langCode }
        val base = code.substringBefore('-')
        if (base != code) {
            availableLanguages.firstOrNull { it.langCode.equals(base, ignoreCase = true) }
                ?.let { return it.langCode }
        }
        return code
    }

    /**
     * Inverse of [toAndroidResFolderCode]: converts an Android resource folder qualifier back into a
     * plain locale code so it can be matched against [availableLanguages].
     * - `pt-rBR` → `pt-BR`, `zh-rCN` → `zh-CN` (region subtag, strip the `r` prefix)
     * - `b+ms+Arab` → `ms-Arab`, `b+es+419` → `es-419` (BCP47 form)
     * Idempotent: plain codes (`pt`, `zh-CN`, `ms-Arab`) pass through unchanged.
     */
    fun fromAndroidResFolderCode(code: String): String {
        if (code.startsWith("b+")) return code.removePrefix("b+").replace('+', '-')

        val parts = code.split('-')
        // Android region form lang-rYY -> lang-YY.
        if (parts.size == 2 && parts[1].matches(Regex("r[A-Za-z]{2}"))) {
            return "${parts[0]}-${parts[1].substring(1).uppercase()}"
        }
        return code
    }

    /**
     * Converts a Google Translate / Locale style language code into a valid Android resource
     * folder qualifier. Android rejects plain region codes such as `pt-BR`; the region subtag
     * must carry an `r` prefix (`pt-rBR`). Script subtags (e.g. `ms-Arab`) and numeric UN M.49
     * regions (e.g. `es-419`) cannot use the `r` form, so they fall back to the BCP47 `b+` form
     * (`b+ms+Arab`), supported by Android resource qualifiers since API 21.
     *
     * Idempotent: codes already in Android form (`zh-rCN`, `b+zh+CN`) are returned unchanged.
     */
    fun toAndroidResFolderCode(code: String): String {
        if (code.startsWith("b+")) return code            // already BCP47
        if (!code.contains('-')) return code              // bare language, e.g. "pt", "fr"

        val parts = code.split('-')
        // Already Android region form (xx-rYY) — leave untouched.
        if (parts.size == 2 && parts[1].matches(Regex("r[A-Z]{2}"))) return code

        if (parts.size != 2) return "b+" + parts.joinToString("+")

        val (lang, sub) = parts
        return when {
            // 2-letter ISO 3166-1 region -> language-rREGION (conventional, all API levels)
            sub.length == 2 && sub.all { it.isLetter() } -> "$lang-r${sub.uppercase()}"
            // numeric region or script subtag -> BCP47
            else -> "b+$lang+$sub"
        }
    }


    fun parseXml(xmlContent: String): Map<String, String> {
        val keyValuePairs = mutableMapOf<String, String>()
        val root = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(ByteArrayInputStream(xmlContent.toByteArray(StandardCharsets.UTF_8)))
            .documentElement

        val nodeList = root.getElementsByTagName("string")
        for (i in 0 until nodeList.length) {
            val stringElement = nodeList.item(i) as Element
            val key = stringElement.getAttribute("name")
            val value = stringElement.textContent
            val translatable: String = stringElement.getAttribute("translatable")
            if (key != null && value != null && translatable != "false") {
                keyValuePairs[key] = value

            }
        }
        return keyValuePairs
    }

    fun combineStringsWithLimit(
        strings: List<String>,
        separator: String,
        limit: Int = 3600
    ): List<String> {
        val combinedStrings = mutableListOf<String>()
        var currentChunk = StringBuilder()

        for (s in strings) {
            if (currentChunk.length + separator.length + s.length > limit) {
                combinedStrings.add(currentChunk.toString())
                currentChunk = StringBuilder(s)
            } else {
                if (currentChunk.isNotEmpty()) {
                    currentChunk.append(separator)
                }
                currentChunk.append(s)
            }
        }
        if (currentChunk.isNotEmpty()) {
            combinedStrings.add(currentChunk.toString())
        }

        return combinedStrings
    }

    fun writeXmlToFile(xmlContent: String, filePath: String) {
        try {
            val file = File(filePath)
            file.parentFile.mkdirs()
            file.writeText(xmlContent)
            println("XML written to $filePath")
        } catch (e: Exception) {
            println("An error occurred while writing: ${e.message}")
        }
    }

    /**
     * Merges [newEntries] into the existing target [existingXml], preserving everything already
     * present — existing `<string>` entries, `<string-array>`, `<plurals>`, comments and any
     * `translatable="false"` strings. Only keys that are not already present as a `<string name=…>`
     * element are appended, so a re-run never duplicates or destroys prior content.
     *
     * (Replaces the old `addNewEntriesToXmlNew`, which rebuilt the file from scratch and silently
     * dropped arrays, plurals, comments and non-translatable strings.)
     */
    fun mergeEntriesIntoXml(existingXml: String, newEntries: Map<String, String>): String {
        try {
            val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()

            val doc = if (existingXml.isBlank()) {
                docBuilder.newDocument().apply { appendChild(createElement("resources")) }
            } else {
                docBuilder.parse(ByteArrayInputStream(existingXml.toByteArray(StandardCharsets.UTF_8)))
            }

            val rootElement = doc.documentElement

            // Collect names already declared as <string> so we never append a duplicate.
            val existingNames = mutableSetOf<String>()
            val existingStrings = rootElement.getElementsByTagName("string")
            for (i in 0 until existingStrings.length) {
                existingNames.add((existingStrings.item(i) as Element).getAttribute("name"))
            }

            for ((key, value) in newEntries) {
                if (key in existingNames) continue
                val element = doc.createElement("string")
                element.setAttribute("name", key)
                element.textContent = value
                rootElement.appendChild(element)
            }

            // Strip whitespace-only text nodes so the indenter can re-pretty-print cleanly
            // instead of inheriting the original file's spacing and producing ragged output.
            val blankNodes = XPathFactory.newInstance().newXPath()
                .evaluate("//text()[normalize-space()='']", doc, XPathConstants.NODESET) as NodeList
            for (i in 0 until blankNodes.length) {
                val node = blankNodes.item(i)
                node.parentNode?.removeChild(node)
            }

            val transformer = TransformerFactory.newInstance().newTransformer()
            transformer.setOutputProperty(OutputKeys.INDENT, "yes")
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4")

            val result = StreamResult(StringWriter())
            transformer.transform(DOMSource(doc), result)
            return result.writer.toString()

        } catch (e: Exception) {
            throw IllegalArgumentException("Error occurred while merging XML: ${e.message}")
        }
    }


    fun makeZipFile(zipFilePath: String, tempDir: String) {
        try {
            ZipOutputStream(FileOutputStream(zipFilePath)).use { zipOut ->
                Files.walk(Paths.get(tempDir)).use { paths ->
                    paths.filter { it.toFile().isFile }.forEach { path ->
                        println("Zip file creating path $path")

                        val zipEntry = ZipEntry(Paths.get(tempDir).relativize(path).toString())
                        zipOut.putNextEntry(zipEntry)
                        Files.copy(path, zipOut)
                        zipOut.closeEntry()
                    }
                }
            }
            println("Zip file created at $zipFilePath")
        } catch (e: Exception) {
            println("An error occurred while creating ZIP file: ${e.message}")
        } finally {
        }
    }
}

data class FileXmlData(
    val contents: String,
    val keyValuePairs: Map<String, String>,
    val languageCode: String
)