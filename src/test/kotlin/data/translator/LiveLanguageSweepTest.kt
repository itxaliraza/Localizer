package data.translator

import data.availableLanguages
import data.network.NetworkResponse
import data.translator.api_interface.TranslatorApis
import data.translator.apis.TranslatorApi1Impl
import data.translator.apis.TranslatorApi2Impl
import data.translator.apis.TranslatorApi3Impl
import data.util.LocalizationUtils.TokenStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Live sweep of EVERY language in the app through the real sanitizer → real Google endpoints → restore
 * path, checking that placeholders and markup come back exactly once. Hits the network for ~250 languages,
 * so it is skipped unless `LIVE_SWEEP=1`:
 *
 *     LIVE_SWEEP=1 ./gradlew test --tests "*LiveLanguageSweepTest*"
 *
 * All languages (including the ones flagged `onlyWebTranslate`) go through the real routing: JSON endpoints
 * first, HTML scraper as the fallback. Report: `build/reports/live-language-sweep.txt`.
 */
class LiveLanguageSweepTest {

    /** Records which token style each (language, string) was sent with, keyed by the sent text. */
    private class Recording(
        private val delegate: TranslatorApis,
        val sent: ConcurrentHashMap<String, MutableList<String>>,
    ) : TranslatorApis {
        override suspend fun getTranslation(fromLanguage: String?, toLanguage: String?, query: String?): NetworkResponse<String> {
            sent.getOrPut(toLanguage.orEmpty()) { java.util.Collections.synchronizedList(mutableListOf()) }.add(query.orEmpty())
            return delegate.getTranslation(fromLanguage, toLanguage, query)
        }
    }

    private val strings = listOf(
        "You have %1\$d new messages from %2\$d people",
        "Click <b>here</b> for %1\$s now, please",
    )

    private fun count(s: String, sub: String) = s.windowed(sub.length).count { it == sub }

    private fun problem(source: String, out: String): String? {
        for (ph in listOf("%1\$d", "%2\$d", "%1\$s")) {
            if (source.contains(ph) && count(out, ph) != 1) return "placeholder $ph appears ${count(out, ph)}x"
        }
        if (source.contains("<b>") && (count(out, "<b>") != 1 || count(out, "</b>") != 1)) return "<b> markup damaged"
        return null
    }

    private fun styleOf(sent: String) = TokenStyle.entries.firstOrNull { sent.contains(it.render(0)) }?.name ?: "?"

    @Test
    fun `every language keeps placeholders and markup intact`() {
        assumeTrue(System.getenv("LIVE_SWEEP") == "1", "set LIVE_SWEEP=1 to run the live language sweep")

        val sent = ConcurrentHashMap<String, MutableList<String>>()
        val json = Recording(TranslatorApi2Impl(), sent)
        val repo = MyTranslatorRepoImpl(
            Recording(TranslatorApi1Impl(), sent), // slot 1 = the HTML scraper (fallback only)
            json,
            Recording(TranslatorApi3Impl(), sent),
        )

        val gate = Semaphore(4)
        val langs = availableLanguages.filter { it.langCode != "en" }
        val results = runBlocking {
            langs.map { lang ->
                async(Dispatchers.IO) {
                    gate.withPermit {
                        lang to strings.map { s ->
                            when (val r = repo.getTranslation(toLanguage = lang, query = s)) {
                                is NetworkResponse.Success -> problem(s, r.data!!)?.let { "BAD($it): ${r.data}" } ?: "OK"
                                else -> "FAIL(${r.error})"
                            }
                        }
                    }
                }
            }.awaitAll()
        }

        fun ok(r: Pair<domain.model.LanguageModel, List<String>>) = r.second.all { it == "OK" }
        val web = results.filter { it.first.onlyWebTranslate }
        val jsonLangs = results.filter { !it.first.onlyWebTranslate }
        val needFallback = results.filter { (l, _) -> sent[l.langCode].orEmpty().any { styleOf(it) != TokenStyle.AT.name } }

        val report = buildString {
            appendLine("routing: JSON endpoints first, HTML scraper as fallback")
            appendLine("all languages: ${results.count(::ok)}/${results.size} ok")
            appendLine("json-endpoint languages: ${jsonLangs.count(::ok)}/${jsonLangs.size} ok")
            appendLine("web-only languages:      ${web.count(::ok)}/${web.size} ok")
            appendLine("languages that needed a fallback token style (${needFallback.size}): " +
                    needFallback.joinToString { (l, _) -> l.langCode + "[" + sent[l.langCode].orEmpty().map(::styleOf).distinct().joinToString(">") + "]" })
            results.filterNot(::ok).forEach { (l, per) ->
                appendLine("${l.langCode} (${if (l.onlyWebTranslate) "web" else "json"}): ${per.joinToString(" | ") { it.take(160) }}")
            }
        }
        File("build/reports").mkdirs()
        File("build/reports/live-language-sweep.txt").writeText(report)
        println(report)

        val damaged = results.flatMap { (l, per) -> per.filter { it.startsWith("BAD") }.map { l.langCode to it } }
        assertTrue(damaged.isEmpty(), "damaged placeholders/markup must never be returned as a success: $damaged")
    }
}
