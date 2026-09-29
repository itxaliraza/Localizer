package data.util

import data.FilesHelper
import data.availableLanguages
import domain.model.LanguageImportResult
import domain.model.LanguageModel

/**
 * Pulls supported languages out of an arbitrary list a user pastes or imports: plain codes (`fr`, `pt-BR`,
 * `zh_CN`), Android folder names (`values-pt-rBR`, `values-b+ms+Arab/strings.xml`) and language names
 * (`French`, `Français`). Everything else in the text (headers, extra CSV columns, JSON keys, XML tags)
 * is ignored, so a spreadsheet export or a `locales_config.xml` can be dropped in as-is.
 */
object LanguageListParser {

    // Cells: one list entry each. Parentheses are NOT separators here so names like
    // "Chinese (Simplified)" stay whole.
    private val cellSeparators = Regex("""[\r\n,;\t|"'`\[\]{}]+""")

    // Words inside a cell that didn't match as a whole, e.g. "French (fr)" or `<locale android:name=`.
    private val wordSeparators = Regex("""[\s()<>=/:]+""")

    // Cells with more words than this are treated as prose and not split, since common short words
    // ("is", "it", "no", "hi") are also language codes.
    private const val MAX_WORDS_TO_SPLIT = 4

    private val codeLike = Regex("""(b\+)?[A-Za-z]{2,3}([-_+][A-Za-z0-9]{2,8})*""")

    // JSON object keys (`"id":`, `"langCodes":`) are field names, never list entries; "id" would
    // otherwise be read as Indonesian.
    private val jsonKey = Regex(""""[^"\n]*"\s*:""")

    private val BYTE_ORDER_MARK = Char(0xFEFF).toString()

    // Codes that name the same language. Android, ISO 639 and Google Translate disagree on these, and the
    // app's list uses one form, so a list written in the other form must still match.
    private val equivalentCodes: List<Set<String>> = listOf(
        setOf("id", "in"),
        setOf("he", "iw"),
        setOf("yi", "ji"),
        setOf("jv", "jw"),
        setOf("fil", "tl"),
        setOf("nb", "no"),
        // Latgalian is `ltg`; the app's list spells it `itg`.
        setOf("ltg", "itg"),
        // `cmn` is Mandarin, the BCP 47 tag some speech/locale APIs use for Chinese.
        setOf("zh", "zh-cn", "zh-hans", "cmn", "cmn-hans"),
        setOf("zh-tw", "zh-hant", "cmn-hant"),
    )

    fun parse(text: String, languages: List<LanguageModel> = availableLanguages): LanguageImportResult {
        val byCode = languages.associateBy { it.langCode.lowercase() }
        val byName = HashMap<String, LanguageModel>()
        languages.forEach { lang ->
            byName.putIfAbsent(lang.langName.trim().lowercase(), lang)
            byName.putIfAbsent(lang.nativeName.trim().lowercase(), lang)
        }

        val matched = LinkedHashSet<LanguageModel>()
        val unrecognized = LinkedHashSet<String>()

        val cleaned = text.removePrefix(BYTE_ORDER_MARK).replace(jsonKey, " ")
        for (rawCell in cleaned.split(cellSeparators)) {
            val cell = normalize(rawCell)
            if (cell.isEmpty()) continue

            val whole = match(cell, byName, byCode)
            if (whole != null) {
                matched += whole
                continue
            }

            val words = cell.split(wordSeparators).map(::normalize).filter { it.isNotEmpty() }
            if (words.size > MAX_WORDS_TO_SPLIT) continue
            for (word in words) {
                val lang = match(word, byName, byCode)
                when {
                    lang != null -> matched += lang
                    // Only report a lone code-like entry; a stray short word inside a phrase is noise.
                    words.size == 1 && codeLike.matches(word) -> unrecognized += word
                }
            }
        }
        return LanguageImportResult(matched.toList(), unrecognized.toList())
    }

    private fun normalize(token: String): String =
        token.trim().removePrefix("values-").removeSuffix("/strings.xml").trim()

    private fun match(
        token: String,
        byName: Map<String, LanguageModel>,
        byCode: Map<String, LanguageModel>,
    ): LanguageModel? {
        byName[token.lowercase()]?.let { return it }
        if (!codeLike.matches(token)) return null
        // Undo Android qualifier forms (pt-rBR → pt-BR, b+ms+Arab → ms-Arab), then resolve.
        val code = FilesHelper.fromAndroidResFolderCode(token.replace('_', '-')).lowercase()
        // Drop subtags from the end until something matches: cmn-hans-cn → cmn-hans → cmn, en-us → en.
        return generateSequence(code) { if ('-' in it) it.substringBeforeLast('-') else null }
            .firstNotNullOfOrNull { resolveCode(it, byCode) }
    }

    /**
     * Exact code, then an equivalent spelling (in ↔ id, jv ↔ jw, …), then the only regional variant
     * the app has for that language (pt → pt-PT when there is no plain `pt`).
     */
    private fun resolveCode(code: String, byCode: Map<String, LanguageModel>): LanguageModel? {
        val candidates = listOf(code) + equivalentCodes.filter { code in it }.flatten()
        candidates.firstNotNullOfOrNull { byCode[it] }?.let { return it }
        return candidates.firstNotNullOfOrNull { candidate ->
            byCode.entries.firstOrNull { it.key.startsWith("$candidate-") }?.value
        }
    }
}
