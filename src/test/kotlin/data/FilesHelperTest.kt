package data

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class FilesHelperTest {

    private fun xml(body: String, rootAttrs: String = "") =
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources$rootAttrs>\n$body\n</resources>"

    // ---- parseXml ---------------------------------------------------------------------------------

    @Test
    fun `parseXml reads strings arrays and plurals in document order`() {
        val parsed = FilesHelper.parseXml(
            xml(
                """
                <string name="hello">Hello</string>
                <string-array name="colors"><item>Red</item><item>Blue</item></string-array>
                <plurals name="apples"><item quantity="one">%d apple</item><item quantity="other">%d apples</item></plurals>
                """.trimIndent()
            )
        )
        assertEquals(
            listOf("hello", "array:colors:0", "array:colors:1", "plurals:apples:one", "plurals:apples:other"),
            parsed.keys.toList()
        )
        assertEquals("Blue", parsed["array:colors:1"])
        assertEquals("%d apples", parsed["plurals:apples:other"])
    }

    @Test
    fun `parseXml skips translatable false on strings arrays and plurals`() {
        val parsed = FilesHelper.parseXml(
            xml(
                """
                <string name="keep">Keep</string>
                <string name="skip" translatable="false">Skip</string>
                <string-array name="skipArr" translatable="false"><item>x</item></string-array>
                <plurals name="skipPl" translatable="false"><item quantity="one">x</item></plurals>
                """.trimIndent()
            )
        )
        assertEquals(listOf("keep"), parsed.keys.toList())
    }

    @Test
    fun `parseXml keeps child markup instead of flattening it`() {
        val parsed = FilesHelper.parseXml(
            xml(
                """
                <string name="rich">Hello <b>bold</b> &amp; <xliff:g id="n" example="Bob">%1${'$'}s</xliff:g>!</string>
                <string name="cdata"><![CDATA[<i>hi</i>]]></string>
                """.trimIndent(),
                rootAttrs = " xmlns:xliff=\"urn:oasis:names:tc:xliff:document:1.2\""
            )
        )
        // The JDK DOM keeps attributes sorted by name, so `id, example` comes back as `example, id`.
        assertEquals("Hello <b>bold</b> &amp; <xliff:g example=\"Bob\" id=\"n\">%1\$s</xliff:g>!", parsed["rich"])
        assertEquals("<![CDATA[<i>hi</i>]]>", parsed["cdata"])
    }

    @Test
    fun `groupOf identifies array and plurals members only`() {
        assertEquals("array:colors", FilesHelper.groupOf("array:colors:3"))
        assertEquals("plurals:apples", FilesHelper.groupOf("plurals:apples:one"))
        assertEquals(null, FilesHelper.groupOf("hello"))
    }

    // ---- mergeEntriesIntoXml ----------------------------------------------------------------------

    @Test
    fun `merge adds only missing strings and keeps existing content`() {
        val existing = xml(
            """
            <!-- keep me -->
            <string name="a">Hola</string>
            <string name="brand" translatable="false">Brand</string>
            """.trimIndent()
        )
        val merged = FilesHelper.mergeEntriesIntoXml(existing, mapOf("a" to "SHOULD NOT REPLACE", "b" to "Nuevo"))
        assertTrue(merged.contains("<!-- keep me -->"), merged)
        assertTrue(merged.contains("<string name=\"a\">Hola</string>"), merged)
        assertFalse(merged.contains("SHOULD NOT REPLACE"), merged)
        assertTrue(merged.contains("<string name=\"brand\" translatable=\"false\">Brand</string>"), merged)
        assertTrue(merged.contains("<string name=\"b\">Nuevo</string>"), merged)
    }

    @Test
    fun `merge into an empty file produces the standard header and layout`() {
        val merged = FilesHelper.mergeEntriesIntoXml(FilesHelper.EMPTY_STRINGS_XML, mapOf("a" to "Uno"))
        assertEquals(
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n    <string name=\"a\">Uno</string>\n</resources>\n",
            merged
        )
    }

    @Test
    fun `merge writes markup as real child nodes`() {
        val merged = FilesHelper.mergeEntriesIntoXml(
            FilesHelper.EMPTY_STRINGS_XML,
            mapOf("rich" to "Hola <b>negrita</b> &amp; más")
        )
        assertTrue(merged.contains("<string name=\"rich\">Hola <b>negrita</b> &amp; más</string>"), merged)
        // parses back to the same value
        assertEquals("Hola <b>negrita</b> &amp; más", FilesHelper.parseXml(merged)["rich"])
    }

    @Test
    fun `merge adds the xliff namespace when a value uses xliff tags`() {
        val merged = FilesHelper.mergeEntriesIntoXml(
            FilesHelper.EMPTY_STRINGS_XML,
            mapOf("msg" to "<xliff:g id=\"n\">%1\$s</xliff:g> te escribió")
        )
        assertTrue(merged.contains("xmlns:xliff=\"urn:oasis:names:tc:xliff:document:1.2\""), merged)
        assertEquals("<xliff:g id=\"n\">%1\$s</xliff:g> te escribió", FilesHelper.parseXml(merged)["msg"])
    }

    @Test
    fun `merge keeps whitespace inside mixed content strings exactly`() {
        val value = "Hello <b>x</b> y  z"
        val merged = FilesHelper.mergeEntriesIntoXml(FilesHelper.EMPTY_STRINGS_XML, mapOf("a" to value))
        assertTrue(merged.contains("<string name=\"a\">$value</string>"), merged)
    }

    @Test
    fun `merge preserves a whitespace-only string`() {
        val existing = xml("<string name=\"space\"> </string>")
        val merged = FilesHelper.mergeEntriesIntoXml(existing, mapOf("b" to "B"))
        assertTrue(merged.contains("<string name=\"space\"> </string>"), merged)
    }

    @Test
    fun `merge creates a new string-array whole and appends missing tail to an existing one`() {
        val created = FilesHelper.mergeEntriesIntoXml(
            FilesHelper.EMPTY_STRINGS_XML,
            mapOf(FilesHelper.arrayItemKey("colors", 0) to "Rojo", FilesHelper.arrayItemKey("colors", 1) to "Azul")
        )
        assertEquals(
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n" +
                    "    <string-array name=\"colors\">\n        <item>Rojo</item>\n        <item>Azul</item>\n    </string-array>\n" +
                    "</resources>\n",
            created
        )

        val partial = xml("<string-array name=\"colors\"><item>Rojo</item></string-array>")
        val extended = FilesHelper.mergeEntriesIntoXml(partial, mapOf(FilesHelper.arrayItemKey("colors", 1) to "Azul"))
        assertEquals(listOf("Rojo", "Azul"), listOf(0, 1).map { FilesHelper.parseXml(extended)[FilesHelper.arrayItemKey("colors", it)] })
    }

    @Test
    fun `merge never appends an array item across a gap`() {
        // item 1 is missing, item 2 present: appending item 2 would put it at position 1.
        val merged = FilesHelper.mergeEntriesIntoXml(
            FilesHelper.EMPTY_STRINGS_XML,
            mapOf(FilesHelper.arrayItemKey("colors", 0) to "Rojo", FilesHelper.arrayItemKey("colors", 2) to "Verde")
        )
        val parsed = FilesHelper.parseXml(merged)
        assertEquals(setOf(FilesHelper.arrayItemKey("colors", 0)), parsed.keys)
    }

    @Test
    fun `merge does not leave an empty array behind`() {
        val merged = FilesHelper.mergeEntriesIntoXml(
            FilesHelper.EMPTY_STRINGS_XML,
            mapOf(FilesHelper.arrayItemKey("colors", 3) to "Verde")
        )
        assertFalse(merged.contains("string-array"), merged)
    }

    @Test
    fun `merge adds missing plural quantities without touching existing ones`() {
        val existing = xml("<plurals name=\"apples\"><item quantity=\"one\">%d manzana</item></plurals>")
        val merged = FilesHelper.mergeEntriesIntoXml(
            existing,
            mapOf(
                FilesHelper.pluralItemKey("apples", "one") to "IGNORED",
                FilesHelper.pluralItemKey("apples", "other") to "%d manzanas"
            )
        )
        val parsed = FilesHelper.parseXml(merged)
        assertEquals("%d manzana", parsed[FilesHelper.pluralItemKey("apples", "one")])
        assertEquals("%d manzanas", parsed[FilesHelper.pluralItemKey("apples", "other")])
    }

    @Test
    fun `merge is idempotent`() {
        val entries = mapOf(
            "a" to "Uno <b>x</b>",
            FilesHelper.arrayItemKey("colors", 0) to "Rojo",
            FilesHelper.pluralItemKey("apples", "other") to "%d manzanas",
        )
        val once = FilesHelper.mergeEntriesIntoXml(FilesHelper.EMPTY_STRINGS_XML, entries)
        val twice = FilesHelper.mergeEntriesIntoXml(once, entries)
        assertEquals(once, twice)
    }

    @Test
    fun `merge output round-trips through parseXml with all entries intact`() {
        val entries = linkedMapOf(
            "a" to "Tom &amp; Jerry",
            "b" to "Don\\'t",
            FilesHelper.arrayItemKey("x", 0) to "One",
            FilesHelper.pluralItemKey("p", "few") to "%d few",
        )
        val merged = FilesHelper.mergeEntriesIntoXml(FilesHelper.EMPTY_STRINGS_XML, entries)
        assertEquals(entries, FilesHelper.parseXml(merged))
    }

    @Test
    fun `merge keeps a malformed value as literal text without double escaping`() {
        val merged = FilesHelper.mergeEntriesIntoXml(FilesHelper.EMPTY_STRINGS_XML, mapOf("a" to "x <b> y &amp; z"))
        assertTrue(merged.contains("<string name=\"a\">x &lt;b&gt; y &amp; z</string>"), merged)
    }

    @Test
    fun `merge into malformed existing xml throws`() {
        assertFailsWith<IllegalArgumentException> {
            FilesHelper.mergeEntriesIntoXml("<resources><string name=\"a\">", mapOf("b" to "B"))
        }
    }

    // ---- writeXmlToFile ---------------------------------------------------------------------------

    @Test
    fun `writeXmlToFile creates folders writes utf-8 and leaves no temp file`() {
        val dir = Files.createTempDirectory("fh-write").toFile()
        try {
            val target = File(dir, "res/values-fr/strings.xml")
            FilesHelper.writeXmlToFile("<resources>é日本</resources>", target.path)
            assertEquals("<resources>é日本</resources>", target.readText(Charsets.UTF_8))
            assertEquals(listOf("strings.xml"), target.parentFile.list()!!.toList())

            // overwrites an existing file
            FilesHelper.writeXmlToFile("<resources/>", target.path)
            assertEquals("<resources/>", target.readText())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `writeXmlToFile throws instead of swallowing failures`() {
        val dir = Files.createTempDirectory("fh-write-fail").toFile()
        try {
            val blocker = File(dir, "values-fr").apply { writeText("i am a file, not a folder") }
            assertFailsWith<Exception> {
                FilesHelper.writeXmlToFile("<resources/>", File(blocker, "strings.xml").path)
            }
        } finally {
            dir.deleteRecursively()
        }
    }

    // ---- language folders / codes -----------------------------------------------------------------

    @Test
    fun `isLanguageFolder accepts base and real language folders`() {
        listOf("values", "values-fr", "values-de", "values-pt-rBR", "values-zh-rCN", "values-in", "values-b+ms+Arab")
            .forEach { assertTrue(FilesHelper.isLanguageFolder(it), it) }
    }

    @Test
    fun `isLanguageFolder rejects configuration qualifiers and non-values folders`() {
        listOf(
            "values-night", "values-v29", "values-sw600dp", "values-w820dp", "values-land", "values-xhdpi",
            "values-fr-night", "values-night-v29", "values-mcc310", "values-ldrtl",
            "values-en", "values-en-rGB",          // must not shadow the base `values`
            "values-xx",                            // not a supported language
            "drawable", "layout", "values-", "valuesfr",
        ).forEach { assertFalse(FilesHelper.isLanguageFolder(it), it) }
    }

    @Test
    fun `extractLanguageCode still resolves android qualifier forms`() {
        // Only Portuguese (Portugal) is offered, so a Brazilian folder resolves to it.
        assertEquals("pt-PT", FilesHelper.extractLanguageCode("values-pt-rBR/strings.xml").second)
        assertEquals("zh-CN", FilesHelper.extractLanguageCode("values-zh-rCN/strings.xml").second)
        assertEquals("in", FilesHelper.extractLanguageCode("values-in/strings.xml").second)
        assertEquals("in", FilesHelper.extractLanguageCode("values-id/strings.xml").second)
        assertEquals("ltg", FilesHelper.extractLanguageCode("values-itg/strings.xml").second)
        assertEquals("en", FilesHelper.extractLanguageCode("values/strings.xml").second)
        assertNotEquals("en", FilesHelper.extractLanguageCode("values-fr/strings.xml").second)
    }
}
