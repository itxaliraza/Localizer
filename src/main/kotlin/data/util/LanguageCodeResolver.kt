package data.util

import data.availableLanguages
import domain.model.LanguageModel

/**
 * Maps any spelling of a language code to the language the app supports, whatever form the app's list
 * uses. Shared by folder detection (`values-in`), list import and saved templates, so an old or
 * alternative code (`id`, `pt`, `itg`, `cmn-Hans-CN`) still finds its language after the list changes.
 */
object LanguageCodeResolver {

    // Codes that name the same language. Android, ISO 639 and Google Translate disagree on these.
    private val equivalentCodes: List<Set<String>> = listOf(
        setOf("id", "in"),
        setOf("he", "iw"),
        setOf("yi", "ji"),
        setOf("jv", "jw"),
        setOf("fil", "tl"),
        setOf("nb", "no"),
        // Latgalian is `ltg`; older app versions (and saved templates) used the typo `itg`.
        setOf("ltg", "itg"),
        // `cmn` is Mandarin, the BCP 47 tag some speech/locale APIs use for Chinese.
        setOf("zh", "zh-cn", "zh-hans", "cmn", "cmn-hans"),
        // Hong Kong and Macau write Traditional Chinese.
        setOf("zh-tw", "zh-hant", "cmn-hant", "zh-hk", "zh-mo"),
    )

    private val defaultByCode: Map<String, LanguageModel> by lazy { index(availableLanguages) }

    fun index(languages: List<LanguageModel>): Map<String, LanguageModel> =
        languages.associateBy { it.langCode.lowercase() }

    /** The supported language for [code] (a plain locale code, not an Android qualifier), or null. */
    fun resolve(code: String, byCode: Map<String, LanguageModel> = defaultByCode): LanguageModel? {
        // Drop subtags from the end until something matches: cmn-hans-cn → cmn-hans → cmn, en-us → en.
        return generateSequence(code.trim().replace('_', '-').lowercase()) {
            if ('-' in it) it.substringBeforeLast('-') else null
        }.firstNotNullOfOrNull { resolveExact(it, byCode) }
    }

    /**
     * Exact code, then an equivalent spelling (in ↔ id, jv ↔ jw, …), then the only regional variant
     * the app has for that language (pt → pt-PT when there is no plain `pt`).
     */
    private fun resolveExact(code: String, byCode: Map<String, LanguageModel>): LanguageModel? {
        val candidates = listOf(code) + equivalentCodes.filter { code in it }.flatten()
        candidates.firstNotNullOfOrNull { byCode[it] }?.let { return it }
        return candidates.firstNotNullOfOrNull { candidate ->
            byCode.entries.firstOrNull { it.key.startsWith("$candidate-") }?.value
        }
    }
}
