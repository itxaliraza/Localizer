package data.util

import domain.model.LanguageModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LanguageListParserTest {

    private fun codes(text: String) = LanguageListParser.parse(text).matched.map { it.langCode }

    @Test
    fun `plain codes separated by commas, newlines and semicolons`() {
        assertEquals(listOf("fr", "de", "es", "ja"), codes("fr, de\nes;ja"))
    }

    @Test
    fun `duplicates are removed and first-seen order is kept`() {
        assertEquals(listOf("de", "fr"), codes("de, fr, de, FR"))
    }

    @Test
    fun `android folder names and region forms resolve to available codes`() {
        val result = codes("values-fr\nvalues-pt-rBR/strings.xml\nvalues-b+ms+Arab\nzh_CN\nvalues-in")
        assertEquals(listOf("fr", "pt-PT", "ms-Arab", "zh-CN", "in"), result)
    }

    @Test
    fun `language names and native names are matched`() {
        assertEquals(listOf("fr", "de", "ja"), codes("French\nDeutsch\nJapanese"))
    }

    @Test
    fun `csv export keeps only the language column values`() {
        val csv = """
            Language,Code,Enabled
            French,fr,Yes
            German,de,Yes
        """.trimIndent()
        val result = LanguageListParser.parse(csv)
        assertEquals(listOf("fr", "de"), result.matched.map { it.langCode })
        // "Yes" looks like a 3-letter code but matches nothing, so it's reported, not silently dropped.
        assertEquals(listOf("Yes"), result.unrecognized)
    }

    @Test
    fun `json array and template export are understood`() {
        assertEquals(listOf("fr", "ko"), codes("""["fr", "ko"]"""))
        assertEquals(
            listOf("it", "nl"),
            codes("""[{"id": "1", "name": "Europe", "langCodes": ["it", "nl"]}]""")
        )
    }

    @Test
    fun `android locales_config xml is understood`() {
        val xml = """
            <locale-config xmlns:android="http://schemas.android.com/apk/res/android">
                <locale android:name="en-US"/>
                <locale android:name="fr"/>
            </locale-config>
        """.trimIndent()
        assertEquals(listOf("en", "fr"), codes(xml))
    }

    @Test
    fun `name with code in parentheses matches`() {
        assertEquals(listOf("fr"), codes("French (fr)"))
    }

    @Test
    fun `unknown code-like entries are reported`() {
        val result = LanguageListParser.parse("fr\nxx\nqq-ZZ")
        assertEquals(listOf("fr"), result.matched.map { it.langCode })
        assertEquals(listOf("xx", "qq-ZZ"), result.unrecognized)
    }

    @Test
    fun `equivalent codes and regional variants resolve whatever form the app list uses`() {
        val list = listOf(
            LanguageModel("Indonesian", "Bahasa Indonesia", "in"),
            LanguageModel("Portuguese (Portugal)", "Português", "pt-PT"),
            LanguageModel("Javanese", "Jawa", "jw"),
            LanguageModel("Hebrew", "עִברִית", "iw"),
        )
        val result = LanguageListParser.parse("id\nin\npt\npt-BR\njv\nhe", list)
        assertEquals(listOf("in", "pt-PT", "jw", "iw"), result.matched.map { it.langCode })
        assertTrue(result.unrecognized.isEmpty())
    }

    @Test
    fun `kotlin source language list is fully recognised`() {
        // Rows from a real app's LanguageModel list: extra locale/speech columns (cmn-Hans-CN, fil-PH, nb-NO),
        // corrected codes (ltg, jv, he, id) and flag references that must be ignored.
        val text = javaClass.getResource("/import/kotlin_language_list.txt")!!.readText()
        val result = LanguageListParser.parse(text)
        assertEquals(emptyList(), result.unrecognized)
        val codes = result.matched.map { it.langCode }.toSet()
        listOf("en", "ab", "ace", "af", "br", "yue", "zh-CN", "fa", "tl", "iw", "in", "jw", "ltg", "ms-Arab",
            "mni-Mtei", "no", "pt-PT", "sat-Latn", "ber-Latn", "yi", "zu")
            .forEach { assertTrue(it in codes, "missing $it") }
    }

    @Test
    fun `mandarin tags map to the right chinese`() {
        assertEquals(listOf("zh-CN", "zh-TW"), codes("cmn-Hans-CN\ncmn-Hant-TW"))
    }

    @Test
    fun `prose is not split into accidental language codes`() {
        assertTrue(codes("this is my list of words and it is not a language list").isEmpty())
    }

    @Test
    fun `empty input gives an empty result`() {
        val result = LanguageListParser.parse("   \n ")
        assertTrue(result.matched.isEmpty())
        assertTrue(result.unrecognized.isEmpty())
    }
}

class UpdateCheckerTest {

    @Test
    fun `newer versions are detected numerically`() {
        assertTrue(UpdateChecker.isNewer("9.0.2", "9.0.1"))
        assertTrue(UpdateChecker.isNewer("10.0.0", "9.9.9"))
        assertTrue(UpdateChecker.isNewer("v9.1", "9.0.9"))
        assertFalse(UpdateChecker.isNewer("9.0.1", "9.0.1"))
        assertFalse(UpdateChecker.isNewer("9.0.0", "9.0.1"))
    }
}

class LanguageCodeResolverTest {

    private fun resolve(code: String) = LanguageCodeResolver.resolve(code)?.langCode

    @Test
    fun `old and alternative spellings resolve to the codes the app uses`() {
        assertEquals("in", resolve("id"))
        assertEquals("in", resolve("in"))
        assertEquals("ltg", resolve("itg"))
        assertEquals("pt-PT", resolve("pt"))
        assertEquals("pt-PT", resolve("pt-BR"))
        assertEquals("iw", resolve("he"))
        assertEquals("zh-CN", resolve("zh"))
        assertEquals("zh-TW", resolve("zh-HK"))
        assertEquals("en", resolve("en_US"))
    }

    @Test
    fun `unknown codes resolve to nothing`() {
        assertEquals(null, resolve("xx"))
        assertEquals(null, resolve("qq-ZZ"))
    }
}
