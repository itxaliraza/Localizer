package data.translator

import data.FilesHelper
import data.availableLanguages
import data.model.TranslationResult
import data.network.NetworkResponse
import data.translator.api_interface.TranslatorApis
import data.translator.apis.TranslatorApi1Impl
import data.translator.apis.TranslatorApi2Impl
import data.translator.apis.TranslatorApi3Impl
import data.util.FolderExtractor
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test

class ScaleTmpTest {

    private class Counting(val name: String, val d: TranslatorApis, val stats: ConcurrentHashMap<String, AtomicInteger>) : TranslatorApis {
        override suspend fun getTranslation(fromLanguage: String?, toLanguage: String?, query: String?): NetworkResponse<String> {
            val r = d.getTranslation(fromLanguage, toLanguage, query)
            val key = if (r is NetworkResponse.Success) "$name ok" else "$name FAIL ${r.error.orEmpty().take(60)}"
            stats.getOrPut(key) { AtomicInteger() }.incrementAndGet()
            return r
        }
    }

    private fun xml(s: String) = s.replace("&", "&amp;")

    @Test
    fun scale() {
        val topics = listOf("tags", "history", "favorites", "notifications", "downloads", "languages", "voice notes", "documents", "themes", "reminders")
        val verbs = listOf("Manage", "Delete", "Share", "Export", "Restore", "Rename", "Open", "Sync")
        val strings = linkedMapOf<String, String>()
        strings["manage_tags"] = "Manage tags"
        strings["tip"] = "Tip: long press a tag to rename or delete it"
        strings["remove_tag"] = "This tag will be removed from all documents. This cannot be undone."
        strings["delete_tag_confirmation"] = "Delete \\\"%1\$s\\\"? This will remove it from %2\$d documents."
        strings["duration"] = "%1\$d min %2\$d sec"
        strings["dont"] = "Don\\'t show this again"
        strings["premium"] = "Go <b>premium</b> & unlock every feature"
        strings["terms"] = "By continuing you accept the <a href=\"https://example.com\">terms</a> and privacy policy."
        strings["wifi"] = "Download only over Wi-Fi"
        strings["about"] = "About this app. Tap to copy diagnostic info for support."
        var i = 0
        for (t in topics) for (v in verbs) {
            if (strings.size >= 69) break
            i++
            strings["s_$i"] = when (i % 4) {
                0 -> "$v your $t. This can take a moment depending on your connection."
                1 -> "$v $t"
                2 -> "$v %1\$d $t now, please"
                else -> "Could not ${v.lowercase()} $t. Please try again later."
            }
        }
        val body = strings.entries.joinToString("\n") { """<string name="${it.key}">${xml(it.value)}</string>""" } +
                """<plurals name="items"><item quantity="one">%d item</item><item quantity="other">%d items</item></plurals>"""

        val res = Files.createTempDirectory("scale").toFile()
        try {
            File(res, "values").mkdirs()
            File(res, "values/strings.xml").writeText("<resources>\n$body\n</resources>")

            val codes = listOf("az", "km", "om", "pa", "pt", "sv", "tk", "fr", "sr", "is", "hi", "am", "ar", "ja", "zh-CN", "tt", "ug", "or", "gu", "ky", "ru", "de", "es", "it")
            val langs = availableLanguages.filter { it.langCode in codes }
            val stats = ConcurrentHashMap<String, AtomicInteger>()
            val repo = MyTranslatorRepoImpl(
                Counting("api1", TranslatorApi1Impl(), stats),
                Counting("api2", TranslatorApi2Impl(), stats),
                Counting("api3", TranslatorApi3Impl(), stats),
            )
            val t0 = System.currentTimeMillis()
            val results = runBlocking {
                val modules = FolderExtractor.extractModules(res.path)
                TranslationManager(repo).translate(langs, modules, true).toList()
            }
            val secs = (System.currentTimeMillis() - t0) / 1000
            val done = results.last() as TranslationResult.TranslationCompleted

            val expected = strings.size + 2 // + plural items
            val perLang = langs.map { l ->
                val f = File(res, "values-${data.FilesHelper.toAndroidResFolderCode(l.langCode)}/strings.xml")
                l.langCode to if (f.exists()) FilesHelper.parseXml(f.readText()).size else -1
            }
            val report = buildString {
                appendLine("languages=${langs.size} keys/lang=$expected elapsed=${secs}s")
                appendLine("translated=${done.translatedKeys} failed=${done.failedKeys} issues=${done.issues.size}")
                done.issues.forEach { appendLine("  ISSUE: $it") }
                appendLine("requests by outcome:")
                stats.entries.sortedBy { it.key }.forEach { appendLine("  ${it.key}: ${it.value}") }
                appendLine("files (lang=strings written, -1 = no file):")
                appendLine(perLang.joinToString(" ") { "${it.first}=${it.second}" })
                appendLine("incomplete: " + perLang.filter { it.second != expected }.joinToString { "${it.first}=${it.second}" })
            }
            File("build/reports").mkdirs()
            File("build/reports/scale.txt").writeText(report)
            println(report)
        } finally {
            res.deleteRecursively()
        }
    }
}
