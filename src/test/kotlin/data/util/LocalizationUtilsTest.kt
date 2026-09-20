package data.util

import data.util.LocalizationUtils.hasTranslatableText
import data.util.LocalizationUtils.restoreAfterTranslation
import data.util.LocalizationUtils.sanitizeForTranslation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocalizationUtilsTest {

    /** Sanitize → (identity "translation") → restore must give the original back. */
    private fun assertRoundTrip(value: String, expectedTokens: Int? = null) {
        val sanitized = sanitizeForTranslation(value)
        if (expectedTokens != null) assertEquals(expectedTokens, sanitized.tokens.size, "tokens for: $value")
        assertEquals(value, restoreAfterTranslation(sanitized.text, sanitized), "round trip of: $value")
    }

    @Test
    fun `plain text has no tokens and round-trips`() = assertRoundTrip("Hello world", expectedTokens = 0)

    @Test
    fun `positional and non-positional format specifiers are protected`() {
        val sanitized = sanitizeForTranslation("Hello %1\$s, you have %d items (%.1f%%) {name}")
        assertEquals(5, sanitized.tokens.size)
        assertFalse(sanitized.text.contains('%'), sanitized.text)
        assertFalse(sanitized.text.contains('{'), sanitized.text)
        assertRoundTrip("Hello %1\$s, you have %d items (%.1f%%) {name}", expectedTokens = 5)
    }

    @Test
    fun `a literal percent sign is not a placeholder`() {
        assertRoundTrip("Get 50% off today", expectedTokens = 0)
        assertRoundTrip("100% sure", expectedTokens = 0)
    }

    @Test
    fun `markup tags are protected`() {
        val sanitized = sanitizeForTranslation("Click <b>here</b> now")
        assertEquals(2, sanitized.tokens.size)
        assertFalse(sanitized.text.contains('<'), sanitized.text)
        assertRoundTrip("Click <b>here</b> now", expectedTokens = 2)
    }

    @Test
    fun `tags glued to words round-trip without gaining or losing spaces`() {
        assertRoundTrip("Click<b>here</b>now")
        assertRoundTrip("<b>Bold</b><i>italic</i>")
        assertRoundTrip("a <b>b</b> c <i>d</i>.")
    }

    @Test
    fun `padding spaces added around tokens for the translator are stripped again`() {
        val sanitized = sanitizeForTranslation("Click <b>here</b>")
        // What a translator returns: words translated, tokens (with our padding) left alone.
        assertEquals("Haga clic <b>aquí</b>", restoreAfterTranslation("Haga clic @0@ aquí @1@", sanitized))
    }

    @Test
    fun `xliff placeholders with attributes round-trip`() {
        assertRoundTrip(
            "<xliff:g id=\"name\" example=\"Bob\">%1\$s</xliff:g> sent you a message",
            expectedTokens = 3,
        )
    }

    @Test
    fun `CDATA markers are protected and the text inside is still translated`() {
        val value = "<![CDATA[Hello <b>world</b>]]>"
        val sanitized = sanitizeForTranslation(value)
        assertTrue(sanitized.translatable)
        assertRoundTrip(value)
    }

    @Test
    fun `XML entities are shown to the translator as plain characters and re-escaped after`() {
        val sanitized = sanitizeForTranslation("Tom &amp; Jerry &lt;3")
        assertEquals("Tom & Jerry <3", sanitized.text)
        assertEquals("Tom &amp; Jerry &lt;3", restoreAfterTranslation("Tom & Jerry <3", sanitized))
    }

    @Test
    fun `android apostrophe and quote escapes are removed for the translator and restored`() {
        val sanitized = sanitizeForTranslation("Don\\'t say \\\"no\\\"")
        assertEquals("Don't say \"no\"", sanitized.text)
        assertEquals("No digas \\\"no\\\" aún \\'ya\\'", restoreAfterTranslation("No digas \"no\" aún 'ya'", sanitized))
        assertRoundTrip("Don\\'t say \\\"no\\\"")
    }

    @Test
    fun `newline escape is not glued to the next word and comes back as newline`() {
        val sanitized = sanitizeForTranslation("Line one\\nLine two")
        assertTrue(sanitized.text.contains("\\n Line"), sanitized.text)
        // translator turns "\n" into "\ n" — restored to "\n"
        assertEquals("Linea uno\\nLinea dos", restoreAfterTranslation("Linea uno\\ nLinea dos", sanitized))
    }

    @Test
    fun `a translation starting with at-sign or question mark is escaped`() {
        val sanitized = sanitizeForTranslation("Handle")
        assertEquals("\\@handle", restoreAfterTranslation("@handle", sanitized))
        assertEquals("\\?", restoreAfterTranslation("?", sanitizeForTranslation("Why")))
        assertRoundTrip("\\@home")
    }

    @Test
    fun `token matching tolerates extra spaces from the translator`() {
        val sanitized = sanitizeForTranslation("Hello %1\$s!")
        assertEquals("Hola %1\$s!", restoreAfterTranslation("Hola @ 0 @ !", sanitized))
    }

    @Test
    fun `restore returns null when a token was dropped, duplicated or invented`() {
        val sanitized = sanitizeForTranslation("Hello %1\$s and %2\$s")
        assertNotNull(restoreAfterTranslation("Hola @0@ y @1@", sanitized))
        assertNull(restoreAfterTranslation("Hola @0@", sanitized), "dropped")
        assertNull(restoreAfterTranslation("Hola @0@ y @0@ y @1@", sanitized), "duplicated")
        assertNull(restoreAfterTranslation("Hola @0@ y @1@ @7@", sanitized), "invented")
        assertNull(restoreAfterTranslation("Hola", sanitized), "all dropped")
    }

    @Test
    fun `restore rejects markup the translator reordered into malformed xml`() {
        val sanitized = sanitizeForTranslation("Click <b>here</b>")
        assertNotNull(restoreAfterTranslation("Haga clic @0@ aquí @1@", sanitized))
        // closing tag before opening tag -> </b>aquí<b> is not well-formed
        assertNull(restoreAfterTranslation("Haga clic @1@ aquí @0@", sanitized))
    }

    @Test
    fun `restore accepts reordered tokens when the markup stays well-formed`() {
        // e.g. Japanese/Korean put a placeholder first; plain placeholders may move freely
        val sanitized = sanitizeForTranslation("Hello %1\$s, you have %2\$d")
        assertEquals("%2\$d ... %1\$s", restoreAfterTranslation("@1@ ... @0@", sanitized))
    }

    @Test
    fun `tokens contain no letters so no language can transliterate or truncate them`() {
        val value = "Hello %1\$s, you have %2\$d <b>new</b> messages"
        for (style in LocalizationUtils.TokenStyle.entries) {
            val sanitized = sanitizeForTranslation(value, style)
            // strip the words we wrote ourselves; whatever is left must be free of letters
            val leftover = sanitized.text.replace("Hello", "").replace("you have", "").replace("new", "").replace("messages", "")
            assertTrue(leftover.none { it.isLetter() }, "$style: $leftover")
        }
    }

    @Test
    fun `every token style round-trips`() {
        val value = "Click <b>here</b> for %1\$s and %2\$d now"
        for (style in LocalizationUtils.TokenStyle.entries) {
            val sanitized = sanitizeForTranslation(value, style)
            assertEquals(4, sanitized.tokens.size, "$style")
            assertEquals(value, restoreAfterTranslation(sanitized.text, sanitized), "$style")
        }
    }

    @Test
    fun `restore only recognises tokens of the style it was sanitized with`() {
        val at = sanitizeForTranslation("Hello %1\$s", LocalizationUtils.TokenStyle.AT)
        // a %n% token from another style is not a token here -> the real one is missing -> rejected
        assertNull(restoreAfterTranslation("Hola %0%", at))
        val percent = sanitizeForTranslation("Hello %1\$s", LocalizationUtils.TokenStyle.PERCENT)
        assertEquals("Hola %1\$s", restoreAfterTranslation("Hola %0%", percent))
    }

    @Test
    fun `tokens with digits localised to another script are still recognised`() {
        val sanitized = sanitizeForTranslation("A %1\$s B %2\$d", LocalizationUtils.TokenStyle.AT)
        // Arabic-Indic digits and Devanagari digits
        assertEquals("أ %1\$s ب %2\$d", restoreAfterTranslation("أ @٠@ ب @١@", sanitized))
        assertEquals("क %1\$s ख %2\$d", restoreAfterTranslation("क @०@ ख @१@", sanitized))
    }

    @Test
    fun `the serbian and icelandic corruptions of the old letter tokens cannot happen`() {
        // Old format sent XXPH1_dXX; Serbian returned КСКСПХ1_дКСКС and Icelandic XXH2_dXX. New tokens have no letters.
        val sanitized = sanitizeForTranslation("You have %1\$d new messages from %2\$d people")
        assertEquals("You have @0@ new messages from @1@ people", sanitized.text)
        assertEquals(
            "Имате %1\$d нових порука од %2\$d особа",
            restoreAfterTranslation("Имате @0@ нових порука од @1@ особа", sanitized)
        )
        assertEquals(
            "Þú ert með %1\$d ný skilaboð frá %2\$d aðilum",
            restoreAfterTranslation("Þú ert með @0@ ný skilaboð frá @1@ aðilum", sanitized)
        )
    }

    @Test
    fun `values with nothing to translate are flagged`() {
        assertFalse(hasTranslatableText("%1\$s"))
        assertFalse(hasTranslatableText("%1\$s / %2\$s"))
        assertFalse(hasTranslatableText("12345"))
        assertFalse(hasTranslatableText("\\n"))
        assertFalse(hasTranslatableText(""))
        assertFalse(hasTranslatableText("@string/app_name"))
        assertFalse(hasTranslatableText("?attr/colorPrimary"))
        assertFalse(hasTranslatableText("@android:string/ok"))
        assertFalse(hasTranslatableText("<b></b>"))
    }

    @Test
    fun `values with prose are translatable`() {
        assertTrue(hasTranslatableText("OK"))
        assertTrue(hasTranslatableText("%1\$s items"))
        assertTrue(hasTranslatableText("Ünïcode text"))
        assertTrue(hasTranslatableText("日本語"))
        assertTrue(hasTranslatableText("<b>Bold</b>"))
    }
}
