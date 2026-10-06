package data.translator

import data.FilesHelper
import data.model.TranslationResult
import data.network.NetworkResponse
import data.util.FolderExtractor
import domain.model.LanguageModel
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TranslationManagerTest {

    private val french = LanguageModel("French", "Français", "fr")
    private val german = LanguageModel("German", "Deutsch", "de")

    private lateinit var res: File

    @BeforeTest
    fun setUp() {
        res = Files.createTempDirectory("tm-test").toFile()
    }

    @AfterTest
    fun tearDown() {
        res.deleteRecursively()
    }

    private fun writeBase(body: String) {
        File(res, "values").mkdirs()
        File(res, "values/strings.xml").writeText("<resources>\n$body\n</resources>")
    }

    /** Repo that answers via [answer]; `null` means the endpoint failed for that value. */
    private fun repo(answer: (LanguageModel, String) -> String?) = object : TranslationRepository {
        val calls = java.util.concurrent.atomic.AtomicInteger()
        override suspend fun getTranslation(fromLanguage: String, toLanguage: LanguageModel, query: String): NetworkResponse<String> {
            calls.incrementAndGet()
            val translated = answer(toLanguage, query) ?: return NetworkResponse.Failure("offline")
            return NetworkResponse.Success(translated)
        }
    }

    private fun execute(
        repo: TranslationRepository,
        languages: List<LanguageModel>,
        parallel: Boolean = true,
    ): List<TranslationResult> = runBlocking {
        val modules = FolderExtractor.extractModules(res.path)
        TranslationManager(repo, retryBackoffMs = 0).translate(languages, modules, parallel).toList()
    }

    private fun completed(results: List<TranslationResult>): TranslationResult.TranslationCompleted =
        results.last() as TranslationResult.TranslationCompleted

    private fun parsed(lang: String): Map<String, String> =
        FilesHelper.parseXml(File(res, "values-$lang/strings.xml").readText())

    // ---- #1: failures are reported, not hidden -----------------------------------------------------

    @Test
    fun `a fully successful run reports no problems`() {
        writeBase("""<string name="a">Hello</string><string name="b">World</string>""")

        val result = completed(execute(repo { l, v -> "[${l.langCode}] $v" }, listOf(french)))

        assertEquals(2, result.translatedKeys)
        assertEquals(0, result.failedKeys)
        assertFalse(result.hasProblems)
        assertEquals(mapOf("a" to "[fr] Hello", "b" to "[fr] World"), parsed("fr"))
    }

    @Test
    fun `when every request fails the run is reported as nothing translated and nothing is written`() {
        writeBase("""<string name="a">Hello</string><string name="b">World</string>""")

        val result = completed(execute(repo { _, _ -> null }, listOf(french)))

        assertEquals(0, result.translatedKeys)
        assertEquals(2, result.failedKeys)
        assertTrue(result.hasProblems)
        assertEquals(1, result.issues.size)
        assertTrue("fr" in result.issues.single(), result.issues.single())
        assertFalse(File(res, "values-fr").exists(), "no empty/partial file should be created")
    }

    @Test
    fun `partial failure writes what succeeded and counts what did not`() {
        writeBase("""<string name="a">Hello</string><string name="b">World</string><string name="c">Again</string>""")

        val result = completed(execute(repo { l, v -> if (v == "World") null else "[${l.langCode}] $v" }, listOf(french)))

        assertEquals(2, result.translatedKeys)
        assertEquals(1, result.failedKeys)
        assertTrue(result.hasProblems)
        assertEquals(setOf("a", "c"), parsed("fr").keys)
    }

    @Test
    fun `a write failure is reported and the run continues with the next language`() {
        writeBase("""<string name="a">Hello</string>""")
        File(res, "values-fr").writeText("a FILE where the folder should be")   // makes fr unwritable

        val result = completed(execute(repo { l, v -> "[${l.langCode}] $v" }, listOf(french, german)))

        assertTrue(result.issues.any { "fr" in it && "write" in it }, result.issues.toString())
        assertEquals(1, result.failedKeys)
        assertEquals(1, result.translatedKeys)       // german still done
        assertEquals(mapOf("a" to "[de] Hello"), parsed("de"))
    }

    @Test
    fun `sequential mode reports failures the same way`() {
        writeBase("""<string name="a">Hello</string>""")
        val result = completed(execute(repo { _, _ -> null }, listOf(french), parallel = false))
        assertEquals(1, result.failedKeys)
    }

    // ---- #4: arrays and plurals --------------------------------------------------------------------

    @Test
    fun `string-arrays and plurals are translated and written`() {
        writeBase(
            """
            <string name="a">Hello</string>
            <string-array name="colors"><item>Red</item><item>Blue</item></string-array>
            <plurals name="apples"><item quantity="one">%d apple</item><item quantity="other">%d apples</item></plurals>
            """.trimIndent()
        )

        val result = completed(execute(repo { l, v -> "[${l.langCode}] $v" }, listOf(french)))

        assertEquals(5, result.translatedKeys)
        val fr = parsed("fr")
        assertEquals("[fr] Red", fr[FilesHelper.arrayItemKey("colors", 0)])
        assertEquals("[fr] Blue", fr[FilesHelper.arrayItemKey("colors", 1)])
        assertEquals("[fr] %d apple", fr[FilesHelper.pluralItemKey("apples", "one")])
        assertEquals("[fr] %d apples", fr[FilesHelper.pluralItemKey("apples", "other")])
    }

    @Test
    fun `an array with a failed item is dropped whole rather than written misaligned`() {
        writeBase(
            """
            <string name="a">Hello</string>
            <string-array name="colors"><item>Red</item><item>Blue</item><item>Green</item></string-array>
            """.trimIndent()
        )

        val result = completed(execute(repo { l, v -> if (v == "Blue") null else "[${l.langCode}] $v" }, listOf(french)))

        val fr = parsed("fr")
        assertEquals(setOf("a"), fr.keys, "no partial array")
        assertEquals(1, result.translatedKeys)
        assertEquals(3, result.failedKeys)   // all three items count as failed
    }

    @Test
    fun `a rerun only translates what is still missing`() {
        writeBase("""<string name="a">Hello</string><string name="b">World</string>""")
        File(res, "values-fr").mkdirs()
        File(res, "values-fr/strings.xml").writeText("<resources><string name=\"a\">Bonjour</string></resources>")
        val fake = repo { l, v -> "[${l.langCode}] $v" }

        val result = completed(execute(fake, listOf(french)))

        assertEquals(1, fake.calls.get())
        assertEquals(mapOf("a" to "Bonjour", "b" to "[fr] World"), parsed("fr"))
        assertFalse(result.hasProblems)
    }

    // ---- #3: markup survives end to end -----------------------------------------------------------

    @Test
    fun `markup in the base string reaches the translated file as real markup`() {
        writeBase("""<string name="rich">Hello <b>bold</b> &amp; more</string>""")

        execute(repo { _, v -> v.replace("Hello", "Bonjour") }, listOf(french))

        assertEquals("Bonjour <b>bold</b> &amp; more", parsed("fr")["rich"])
    }

    // ---- #5: don't write garbage as a translation -------------------------------------------------

    @Test
    fun `a language that gets the source text back is treated as unsupported and not written`() {
        writeBase("""<string name="a">Hello</string><string name="b">World</string><string name="c">Again</string>""")

        val result = completed(execute(repo { _, v -> v }, listOf(french)))

        assertEquals(0, result.translatedKeys)
        assertEquals(3, result.failedKeys)
        assertTrue(result.issues.single().contains("unchanged"), result.issues.single())
        assertFalse(File(res, "values-fr").exists())
    }

    @Test
    fun `unchanged values are fine when only a few strings are identical to the source`() {
        writeBase("""<string name="a">Hello</string><string name="b">OK</string><string name="c">World</string>""")

        val result = completed(execute(repo { l, v -> if (v == "OK") v else "[${l.langCode}] $v" }, listOf(french)))

        assertFalse(result.hasProblems)
        assertEquals("OK", parsed("fr")["b"])
    }

    @Test
    fun `values with nothing to translate do not count towards the unchanged check`() {
        // 3 numeric strings echo back legitimately; the single real string must still be written.
        writeBase(
            """<string name="a">1</string><string name="b">2</string><string name="c">3</string>
               <string name="d">Hello</string>""".trimIndent()
        )

        val result = completed(execute(repo { l, v -> if (v == "Hello") "[${l.langCode}] $v" else v }, listOf(french)))

        assertFalse(result.hasProblems, result.issues.toString())
        assertEquals(4, parsed("fr").size)
    }

    @Test
    fun `english is skipped and progress is emitted before completion`() {
        writeBase("""<string name="a">Hello</string>""")
        val english = LanguageModel("English", "English", "en")

        val results = execute(repo { l, v -> "[${l.langCode}] $v" }, listOf(english, french))

        assertTrue(results.first() is TranslationResult.UpdateProgress)
        assertNotNull(results.filterIsInstance<TranslationResult.UpdateProgress>().firstOrNull { it.translatingLang == "fr" })
        assertTrue(results.filterIsInstance<TranslationResult.UpdateProgress>().none { it.translatingLang == "en" })
        assertFalse(File(res, "values-en").exists())
    }

    // ---- circuit breaker: don't hammer a Google that isn't answering ------------------------------

    private fun manyKeys(n: Int) =
        writeBase((1..n).joinToString("") { """<string name="k$it">Value $it</string>""" })

    @Test
    fun `when google is not answering a language stops early instead of retrying every string`() {
        manyKeys(30)
        val repo = repo { _, _ -> null }

        val result = completed(execute(repo, listOf(french)))

        // without the breaker: 30 strings x 3 attempts = 90 requests
        assertTrue(repo.calls.get() < 60, "made ${repo.calls.get()} requests")
        assertEquals(0, result.translatedKeys)
        assertEquals(30, result.failedKeys, "skipped strings still count as failed")
        assertTrue(result.issues.single().contains("not answering"), result.issues.single())
        assertFalse(File(res, "values-fr").exists())
    }

    @Test
    fun `three blocked languages in a row stop the whole run`() {
        manyKeys(8)
        val attempted = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
        val repo = repo { l, _ -> attempted.add(l.langCode); null }
        val langs = listOf("fr", "de", "es", "it", "nl", "pl").map { LanguageModel(it, it, it) }

        val result = completed(execute(repo, langs))

        assertEquals(3, attempted.size, "languages after the third blocked one must not be attempted: $attempted")
        assertTrue(result.issues.any { it.startsWith("Stopped early") }, result.issues.toString())
        assertEquals(0, result.translatedKeys)
    }

    @Test
    fun `one working language resets the blocked counter so the run continues`() {
        manyKeys(8)
        val attempted = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
        // fr blocked, de fine, es blocked, it fine, nl blocked, pl fine: never 3 blocked in a row
        val repo = repo { l, v -> attempted.add(l.langCode); if (l.langCode in setOf("de", "it", "pl")) "[${l.langCode}] $v" else null }
        val langs = listOf("fr", "de", "es", "it", "nl", "pl").map { LanguageModel(it, it, it) }

        val result = completed(execute(repo, langs))

        assertEquals(6, attempted.size)
        assertFalse(result.issues.any { it.startsWith("Stopped early") }, result.issues.toString())
        assertEquals(24, result.translatedKeys)   // 3 working languages x 8 strings
    }

    @Test
    fun `a language that got even one answer never trips the breaker`() {
        // sequential + success first: 1 success, then 8 failures (more than the trip threshold), then 1 more success
        writeBase(
            """<string name="a">Fine</string>""" +
                    (1..8).joinToString("") { """<string name="bad$it">Bad $it</string>""" } +
                    """<string name="z">Also fine</string>"""
        )
        val repo = repo { l, v -> if (v.startsWith("Bad")) null else "[${l.langCode}] $v" }

        val result = completed(execute(repo, listOf(french), parallel = false))

        assertEquals(2, result.translatedKeys, "the string after the failures must still be tried")
        assertEquals(8, result.failedKeys)
        assertFalse(result.issues.any { it.contains("not answering") }, result.issues.toString())
        assertEquals(setOf("a", "z"), parsed("fr").keys)
    }

    // ---- two folders for one language (values-in + values-id, values-iw + values-he) ------------------

    private val indonesian = LanguageModel("Indonesian", "Bahasa Indonesia", "in")

    private fun writeLang(folder: String, body: String) {
        File(res, folder).mkdirs()
        File(res, "$folder/strings.xml").writeText("<resources>\n$body\n</resources>")
    }

    private fun parsedFolder(folder: String): Map<String, String> =
        FilesHelper.parseXml(File(res, "$folder/strings.xml").readText())

    @Test
    fun `each folder of a language is merged and written on its own, nothing is overwritten`() {
        writeBase(
            """<string name="a">Hello</string>
            <plurals name="left"><item quantity="one">%d left</item><item quantity="other">%d left</item></plurals>"""
        )
        // values-id already has everything, including the plurals; values-in only has the plain string.
        writeLang(
            "values-id",
            """<string name="a">Halo</string>
            <plurals name="left"><item quantity="one">sisa %d</item><item quantity="other">sisa %d</item></plurals>"""
        )
        writeLang("values-in", """<string name="a">Halo</string>""")

        val result = completed(execute(repo { l, v -> "[${l.langCode}] $v" }, listOf(indonesian)))

        // values-id is untouched: its plurals must survive.
        assertEquals(
            mapOf("a" to "Halo", "plurals:left:one" to "sisa %d", "plurals:left:other" to "sisa %d"),
            parsedFolder("values-id")
        )
        // values-in gets the missing plurals added, keeps its string.
        assertEquals(
            mapOf("a" to "Halo", "plurals:left:one" to "[in] %d left", "plurals:left:other" to "[in] %d left"),
            parsedFolder("values-in")
        )
        assertEquals(2, result.translatedKeys)
        assertFalse(result.hasProblems)
    }

    @Test
    fun `a new language with no folder is written to its own qualifier`() {
        writeBase("""<plurals name="left"><item quantity="one">%d left</item><item quantity="other">%d left</item></plurals>""")

        completed(execute(repo { l, v -> "[${l.langCode}] $v" }, listOf(indonesian)))

        assertEquals(
            mapOf("plurals:left:one" to "[in] %d left", "plurals:left:other" to "[in] %d left"),
            parsedFolder("values-in")
        )
    }
}
