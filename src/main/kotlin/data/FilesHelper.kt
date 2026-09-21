package data

import org.w3c.dom.CDATASection
import org.w3c.dom.Comment
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.Text
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory


object FilesHelper {

    /** In-memory stand-in for a language folder that has no `strings.xml` yet (never written on load). */
    const val EMPTY_STRINGS_XML = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n</resources>"

    private const val ARRAY_PREFIX = "array:"
    private const val PLURALS_PREFIX = "plurals:"
    private const val XLIFF_NAMESPACE = "urn:oasis:names:tc:xliff:document:1.2"

    // Android language qualifiers only: `fr`, `pt-rBR`, `b+ms+Arab`. Excludes `night`, `v29`, `sw600dp`,
    // `fr-night`, … which are configuration qualifiers, not languages.
    private val languageQualifierRegex = Regex("""[a-z]{2,3}(-r?[A-Za-z]{2})?|b\+[A-Za-z0-9+]+""")

    private val STRUCTURAL_ELEMENTS = setOf("resources", "string-array", "plurals")


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

    /**
     * True for `values` and for `values-<language>` folders whose language the app supports. Folders for
     * other resource qualifiers (`values-night`, `values-v29`, `values-sw600dp`, …) hold no
     * translatable language and must never be read as one, nor written to. `values-en*` is excluded so it
     * can't shadow the base `values` folder.
     */
    fun isLanguageFolder(folderName: String): Boolean {
        if (folderName == "values") return true
        val qualifier = folderName.removePrefix("values-")
        if (qualifier == folderName || !languageQualifierRegex.matches(qualifier)) return false
        val code = extractLanguageCode("$folderName/strings.xml").second
        return code != "en" && availableLanguages.any { it.langCode == code }
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


    /** Key of item [index] of `<string-array name="[name]">` in the maps returned by [parseXml]. */
    fun arrayItemKey(name: String, index: Int) = "$ARRAY_PREFIX$name:$index"

    /** Key of `<item quantity="[quantity]">` of `<plurals name="[name]">` in the maps returned by [parseXml]. */
    fun pluralItemKey(name: String, quantity: String) = "$PLURALS_PREFIX$name:$quantity"

    /**
     * The array/plurals a key belongs to, or `null` for a plain `<string>`. Items of one group must be
     * translated all-or-nothing: a string-array missing an item shifts every later index.
     */
    fun groupOf(key: String): String? =
        if (key.startsWith(ARRAY_PREFIX) || key.startsWith(PLURALS_PREFIX)) key.substringBeforeLast(':') else null

    /**
     * Extracts every translatable entry of a `strings.xml`, in document order, as `key -> value`:
     * - `<string name="n">`                      → `n`
     * - `<string-array name="n"><item>`          → [arrayItemKey]
     * - `<plurals name="n"><item quantity="q">`  → [pluralItemKey]
     *
     * A value is the element's **inner XML** — text with `&amp;`/`&lt;`/`&gt;` escaped and any child
     * markup (`<b>`, `<xliff:g>`, CDATA) kept — not `textContent`, which would silently flatten markup.
     * Elements marked `translatable="false"` are skipped.
     */
    fun parseXml(xmlContent: String): Map<String, String> {
        val entries = linkedMapOf<String, String>()
        val root = newDocumentBuilder()
            .parse(ByteArrayInputStream(xmlContent.toByteArray(StandardCharsets.UTF_8)))
            .documentElement

        for (element in root.childElements()) {
            if (element.getAttribute("translatable") == "false") continue
            val name = element.getAttribute("name")
            if (name.isEmpty()) continue
            when (element.tagName) {
                "string" -> entries[name] = innerXml(element)
                "string-array" -> element.childElements("item").forEachIndexed { index, item ->
                    entries[arrayItemKey(name, index)] = innerXml(item)
                }
                "plurals" -> element.childElements("item").forEach { item ->
                    val quantity = item.getAttribute("quantity")
                    if (quantity.isNotEmpty()) entries[pluralItemKey(name, quantity)] = innerXml(item)
                }
            }
        }
        return entries
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

    /**
     * Writes [xmlContent] to [filePath] (UTF-8), creating parent folders. The content goes to a temp file
     * first and is moved into place, so a crash can't leave a half-written `strings.xml`.
     * Throws on failure — callers must surface it, a swallowed write error looks like success.
     */
    fun writeXmlToFile(xmlContent: String, filePath: String) {
        val target = File(filePath).absoluteFile
        target.parentFile.mkdirs()
        val temp = File(target.parentFile, target.name + ".tmp")
        try {
            temp.writeText(xmlContent, StandardCharsets.UTF_8)
            try {
                Files.move(
                    temp.toPath(), target.toPath(),
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING
                )
            } catch (e: AtomicMoveNotSupportedException) {
                Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            temp.delete()
        }
    }

    /**
     * Merges [newEntries] (keys as produced by [parseXml]) into the existing target [existingXml],
     * preserving everything already present — strings, arrays, plurals, comments, `translatable="false"`
     * entries. Only what is missing is added: a `<string>` whose name is absent, an array item beyond the
     * array's current length (new arrays are created whole), a plurals `<item>` whose quantity is absent.
     * So a re-run never duplicates or destroys prior content.
     *
     * Values are inner XML (see [parseXml]) and are inserted as real child nodes, so markup survives.
     * Output is produced by [serializeResources] (4-space indent, UTF-8 header) rather than the JDK
     * Transformer, whose indenting can corrupt whitespace inside mixed-content strings.
     */
    fun mergeEntriesIntoXml(existingXml: String, newEntries: Map<String, String>): String {
        try {
            val docBuilder = newDocumentBuilder()
            val doc = if (existingXml.isBlank()) {
                docBuilder.newDocument().apply { appendChild(createElement("resources")) }
            } else {
                docBuilder.parse(ByteArrayInputStream(existingXml.toByteArray(StandardCharsets.UTF_8)))
            }
            val root = doc.documentElement
            var usesXliff = false

            fun appendValue(target: Element, value: String) {
                if (value.contains("<xliff:")) usesXliff = true
                try {
                    val fragment = docBuilder.parse(
                        ByteArrayInputStream("<r>$value</r>".toByteArray(StandardCharsets.UTF_8))
                    )
                    var node = fragment.documentElement.firstChild
                    while (node != null) {
                        target.appendChild(doc.importNode(node, true))
                        node = node.nextSibling
                    }
                } catch (e: Exception) {
                    // Not well-formed markup (callers validate first, so this is a last resort): keep the
                    // entry as literal text. Decode the entities once so the serializer doesn't double-escape.
                    target.appendChild(
                        doc.createTextNode(value.replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&"))
                    )
                }
            }

            fun existing(tag: String, name: String) =
                root.childElements(tag).firstOrNull { it.getAttribute("name") == name }

            val strings = linkedMapOf<String, String>()
            val arrays = linkedMapOf<String, MutableMap<Int, String>>()
            val plurals = linkedMapOf<String, MutableMap<String, String>>()
            for ((key, value) in newEntries) {
                when {
                    key.startsWith(ARRAY_PREFIX) -> {
                        val group = key.removePrefix(ARRAY_PREFIX)
                        val index = group.substringAfterLast(':').toIntOrNull() ?: continue
                        arrays.getOrPut(group.substringBeforeLast(':')) { sortedMapOf() }[index] = value
                    }

                    key.startsWith(PLURALS_PREFIX) -> {
                        val group = key.removePrefix(PLURALS_PREFIX)
                        plurals.getOrPut(group.substringBeforeLast(':')) { linkedMapOf() }[group.substringAfterLast(':')] = value
                    }

                    else -> strings[key] = value
                }
            }

            val existingStringNames = root.childElements("string").map { it.getAttribute("name") }.toSet()
            for ((name, value) in strings) {
                if (name in existingStringNames) continue
                val element = doc.createElement("string")
                element.setAttribute("name", name)
                appendValue(element, value)
                root.appendChild(element)
            }

            for ((name, items) in arrays) {
                val array = existing("string-array", name)
                    ?: doc.createElement("string-array").also { it.setAttribute("name", name); root.appendChild(it) }
                var next = array.childElements("item").size
                // Append only contiguously: a gap would shift every later item's position.
                while (items.containsKey(next)) {
                    val item = doc.createElement("item")
                    appendValue(item, items.getValue(next))
                    array.appendChild(item)
                    next++
                }
                if (array.childElements("item").isEmpty()) root.removeChild(array)
            }

            for ((name, items) in plurals) {
                val element = existing("plurals", name)
                    ?: doc.createElement("plurals").also { it.setAttribute("name", name); root.appendChild(it) }
                val haveQuantities = element.childElements("item").map { it.getAttribute("quantity") }.toSet()
                for ((quantity, value) in items) {
                    if (quantity in haveQuantities) continue
                    val item = doc.createElement("item")
                    item.setAttribute("quantity", quantity)
                    appendValue(item, value)
                    element.appendChild(item)
                }
                if (element.childElements("item").isEmpty()) root.removeChild(element)
            }

            if (usesXliff && !root.hasAttribute("xmlns:xliff")) root.setAttribute("xmlns:xliff", XLIFF_NAMESPACE)

            stripStructuralWhitespace(root)
            return serializeResources(doc)
        } catch (e: Exception) {
            throw IllegalArgumentException("Error occurred while merging XML: ${e.message}", e)
        }
    }


    // ---- XML helpers --------------------------------------------------------------------------------

    private fun newDocumentBuilder(): DocumentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()

    private fun Element.childElements(tag: String? = null): List<Element> {
        val result = mutableListOf<Element>()
        var child = firstChild
        while (child != null) {
            if (child is Element && (tag == null || child.tagName == tag)) result += child
            child = child.nextSibling
        }
        return result
    }

    // Formatting whitespace between structural elements is re-generated on output; whitespace inside a
    // <string> is content and is left alone.
    private fun stripStructuralWhitespace(element: Element) {
        if (element.tagName !in STRUCTURAL_ELEMENTS) return
        var child = element.firstChild
        while (child != null) {
            val next = child.nextSibling
            if (child is Text && child !is CDATASection && child.data.isBlank()) element.removeChild(child)
            else if (child is Element) stripStructuralWhitespace(child)
            child = next
        }
    }

    private fun escapeXmlText(text: String) = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    private fun escapeXmlAttribute(text: String) = escapeXmlText(text).replace("\"", "&quot;")

    private fun startTag(element: Element): String = buildString {
        append('<').append(element.tagName)
        val attributes = element.attributes
        for (i in 0 until attributes.length) {
            val attribute = attributes.item(i)
            append(' ').append(attribute.nodeName).append("=\"")
                .append(escapeXmlAttribute(attribute.nodeValue)).append('"')
        }
    }

    private fun innerXml(element: Element): String = buildString {
        var child = element.firstChild
        while (child != null) {
            appendInline(child)
            child = child.nextSibling
        }
    }

    private fun StringBuilder.appendInline(node: Node) {
        when (node) {
            is CDATASection -> append("<![CDATA[").append(node.data).append("]]>")
            is Text -> append(escapeXmlText(node.data))
            is Comment -> append("<!--").append(node.data).append("-->")
            is Element -> {
                append(startTag(node))
                if (node.firstChild == null) {
                    append("/>")
                } else {
                    append('>').append(innerXml(node)).append("</").append(node.tagName).append('>')
                }
            }

            else -> {
                // Entity references are already expanded by the parser; anything else contributes its children.
                var child = node.firstChild
                while (child != null) {
                    appendInline(child)
                    child = child.nextSibling
                }
            }
        }
    }

    /**
     * Deterministic pretty printer for Android resource files: one entry per line, 4-space indent.
     * `<string>` and `<item>` are always written inline so their whitespace is never altered.
     */
    private fun serializeResources(doc: Document): String {
        val out = StringBuilder("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
        writeElement(out, doc.documentElement, 0)
        return out.toString()
    }

    private fun writeElement(out: StringBuilder, element: Element, depth: Int) {
        val indent = "    ".repeat(depth)
        val hasElementChild = element.childElements().isNotEmpty()
        val hasContentText = generateSequence(element.firstChild) { it.nextSibling }
            .any { it is Text && it.data.isNotBlank() }
        val structured = hasElementChild && !hasContentText && element.tagName != "string" && element.tagName != "item"

        when {
            element.firstChild == null -> out.append(indent).append(startTag(element)).append("/>\n")
            !structured -> out.append(indent).append(startTag(element)).append('>')
                .append(innerXml(element)).append("</").append(element.tagName).append(">\n")

            else -> {
                out.append(indent).append(startTag(element)).append(">\n")
                var child = element.firstChild
                while (child != null) {
                    when (child) {
                        is Element -> writeElement(out, child, depth + 1)
                        is Comment -> out.append(indent).append("    <!--").append(child.data).append("-->\n")
                        else -> {}
                    }
                    child = child.nextSibling
                }
                out.append(indent).append("</").append(element.tagName).append(">\n")
            }
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
