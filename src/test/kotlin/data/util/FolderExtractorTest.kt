package data.util

import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FolderExtractorTest {

    private lateinit var root: File

    @BeforeTest
    fun setUp() {
        root = Files.createTempDirectory("folder-extractor").toFile()
    }

    @AfterTest
    fun tearDown() {
        root.deleteRecursively()
    }

    private fun file(path: String, content: String? = null): File =
        File(root, path).also { f ->
            if (content != null) {
                f.parentFile.mkdirs()
                f.writeText(content)
            } else {
                f.mkdirs()
            }
        }

    private fun strings(vararg entries: Pair<String, String>) =
        "<resources>\n" + entries.joinToString("\n") { (k, v) -> "<string name=\"$k\">$v</string>" } + "\n</resources>"

    /** Every file and folder under [root], relative — to prove loading changed nothing on disk. */
    private fun snapshot(): Set<String> =
        root.walkTopDown().map { it.relativeTo(root).path }.toSet()

    private fun buildResFolder(): File {
        val res = file("res")
        file("res/values/strings.xml", strings("hello" to "Hello"))
        file("res/values-fr/strings.xml", strings("hello" to "Bonjour"))
        file("res/values-de")                                   // language folder WITHOUT strings.xml
        file("res/values-night/colors.xml", "<resources/>")     // qualifier folders: not languages
        file("res/values-v29")
        file("res/values-sw600dp")
        file("res/values-fr-night")
        file("res/drawable")
        return res
    }

    @Test
    fun `loading a res folder never writes to the project`() = runBlocking {
        val res = buildResFolder()
        val before = snapshot()

        FolderExtractor.extractModules(res.path)

        assertEquals(before, snapshot(), "load must not create or modify anything")
        assertTrue(!File(res, "values-de/strings.xml").exists())
        assertTrue(!File(res, "values-night/strings.xml").exists())
        assertTrue(!File(res, "values-v29/strings.xml").exists())
    }

    @Test
    fun `only base and language folders are extracted`() = runBlocking {
        val res = buildResFolder()

        val module = FolderExtractor.extractModules(res.path).single()

        assertEquals(
            setOf("values/strings.xml", "values-fr/strings.xml", "values-de/strings.xml"),
            module.extraction.extractedFiles.keys
        )
    }

    @Test
    fun `a language folder without strings xml is still detected and pre-selected`() = runBlocking {
        val res = buildResFolder()

        val module = FolderExtractor.extractModules(res.path).single()

        assertEquals(setOf("fr", "de"), module.extraction.selectedLangs.map { it.langCode }.filter { it != "en" }.toSet())
        // its in-memory content is an empty <resources/> so translation can merge into it
        assertTrue(module.extraction.extractedFiles.getValue("values-de/strings.xml").contains("<resources>"))
    }

    @Test
    fun `android qualifier folders are mapped back to language codes`() = runBlocking {
        file("res/values/strings.xml", strings("a" to "A"))
        file("res/values-pt-rBR/strings.xml", strings("a" to "A-pt"))
        file("res/values-in/strings.xml", strings("a" to "A-id"))

        val module = FolderExtractor.extractModules(File(root, "res").path).single()

        val codes = module.extraction.selectedLangs.map { it.langCode }
        assertTrue("pt" in codes, codes.toString())
        assertTrue("id" in codes, codes.toString())
        assertEquals("in", module.extraction.changeFileCodes["id"])   // written back to values-in
    }

    @Test
    fun `a project root is split into modules and build output is skipped`() = runBlocking {
        file("app/src/main/res/values/strings.xml", strings("a" to "A"))
        file("lib/src/main/res/values/strings.xml", strings("b" to "B", "c" to "C"))
        file("app/build/intermediates/res/values/strings.xml", strings("generated" to "x"))
        file("app/src/main/res/values-night")

        val before = snapshot()
        val modules = FolderExtractor.extractModules(root.path)

        assertEquals(setOf("app", "lib"), modules.map { it.moduleName }.toSet())
        assertEquals(mapOf("app" to 1, "lib" to 2), modules.associate { it.moduleName to it.baseStringCount })
        assertEquals(before, snapshot())
    }

    @Test
    fun `a folder that is not an android project yields no modules`() = runBlocking {
        file("readme.txt", "nothing here")
        assertTrue(FolderExtractor.extractModules(root.path).isEmpty())
        assertTrue(FolderExtractor.extractModules(File(root, "missing").path).isEmpty())
    }

    @Test
    fun `base string count includes array and plural items`() = runBlocking {
        file(
            "res/values/strings.xml",
            "<resources><string name=\"a\">A</string>" +
                    "<string-array name=\"x\"><item>1</item><item>2</item></string-array>" +
                    "<plurals name=\"p\"><item quantity=\"one\">1</item><item quantity=\"other\">n</item></plurals></resources>"
        )
        assertEquals(5, FolderExtractor.extractModules(File(root, "res").path).single().baseStringCount)
    }
}
