package data.util

import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Protects everything in a `strings.xml` value that must not be touched by the translator, then puts
 * it back afterwards.
 *
 * Values arrive as *inner XML* (see `FilesHelper.parseXml`): text has `&amp;`/`&lt;`/`&gt;` escaped and
 * Android backslash escapes (`\'`, `\n`) intact, markup (`<b>`, `<xliff:g id="x">`, CDATA markers)
 * is present as tags. [sanitizeForTranslation] turns that into plain text for the translator, with
 * every tag and format placeholder replaced by a numbered token (`@3@`). [restoreAfterTranslation]
 * reverses it, re-escaping the text so the result is inner XML again — and returns `null` if the
 * translator lost, duplicated or invented a token, so a damaged string is never written to disk.
 *
 * Tokens contain **no letters**: letter-based tokens (`XXPH1XX`) are transliterated or truncated by some
 * languages (Serbian → `КСКСПХ1КСКС`, Icelandic → `XXH1XX`, also Hindi/Amharic). Digits and symbols are
 * not transliterated. No single symbol style survives every language (Google sometimes deletes a token
 * sitting next to a word it drops), so [TokenStyle] is a ladder: callers retry a damaged result with the
 * next style.
 */
object LocalizationUtils {

    /** How a token is written for the translator. Ordered by measured survival across languages. */
    enum class TokenStyle(val open: String, val close: String) {
        AT("@", "@"),
        PERCENT("%", "%"),
        BRACKETS("[[", "]]"),
        MATH("\u27e6", "\u27e7");

        fun render(index: Int) = "$open$index$close"

        // Tolerant of spaces the translator may add, and of digits in any script (ar/fa/hi/bn localise them).
        internal val regex = Regex(
            """(\s*)${Regex.escape(open)}\s*(\p{Nd}+)\s*${Regex.escape(close)}(\s*)"""
        )
    }

    /** One protected span. [padBefore]/[padAfter]: a space was added next to the token when sending
     *  (so the translator doesn't glue it to a word) and must be removed when restoring. */
    data class Token(val original: String, val padBefore: Boolean, val padAfter: Boolean)

    /** [text] is what is sent to the translator. [translatable] is false when there is nothing to
     *  translate (only tokens, digits, punctuation or a `@string/…` reference) — send nothing. */
    data class SanitizedText(
        val text: String,
        val tokens: List<Token>,
        val translatable: Boolean,
        val style: TokenStyle = TokenStyle.AT,
    )

    // Tags, CDATA markers and comments. Only real tags (`<` + letter) so a stray `<` isn't swallowed.
    private val markupRegex = Regex("""<!\[CDATA\[|\]\]>|<!--.*?-->|</?[A-Za-z][^>]*>""", RegexOption.DOT_MATCHES_ALL)

    // printf-style specifiers (%s, %d, %1$s, %.2f, %02d, %-10s, %%) and {name}/{0} style placeholders.
    // No space flag: "50% off" must not read as the specifier "% o".
    private val placeholderRegex = Regex("""%(?:\d+\$)?[-#+0,(]*\d*(?:\.\d+)?[a-zA-Z%]|\{\w+}""")

    private val resourceReferenceRegex = Regex("""^\s*[@?][\w.:+]*/[\w.]+\s*$""")

    fun sanitizeForTranslation(value: String, style: TokenStyle = TokenStyle.AT): SanitizedText {
        val builder = Builder(style)
        var last = 0
        for (match in markupRegex.findAll(value)) {
            addTextGap(builder, value.substring(last, match.range.first))
            builder.token(match.value)
            last = match.range.last + 1
        }
        addTextGap(builder, value.substring(last))

        val translatable = !resourceReferenceRegex.matches(value) && builder.hasLetters
        return SanitizedText(builder.out.toString(), builder.tokens, translatable, style)
    }

    /** True when [value] contains prose worth translating (see [SanitizedText.translatable]). */
    fun hasTranslatableText(value: String): Boolean = sanitizeForTranslation(value).translatable

    /**
     * Rebuilds inner XML from the translator's [translated] output, or `null` when the tokens in
     * [sanitized] did not all come back exactly once (or the markup is no longer well-formed).
     */
    fun restoreAfterTranslation(translated: String, sanitized: SanitizedText): String? {
        val tokens = sanitized.tokens
        val seen = BooleanArray(tokens.size)
        val result = StringBuilder()
        var last = 0
        for (match in sanitized.style.regex.findAll(translated)) {
            val index = parseDigits(match.groupValues[2]) ?: return null
            if (index !in tokens.indices || seen[index]) return null
            seen[index] = true
            val token = tokens[index]

            result.append(escapeText(translated.substring(last, match.range.first)))
            if (!token.padBefore) result.append(match.groupValues[1])
            result.append(token.original)
            if (!token.padAfter) result.append(match.groupValues[3])
            last = match.range.last + 1
        }
        if (seen.any { !it }) return null
        result.append(escapeText(translated.substring(last)))

        // A translator may reorder tokens so a closing tag lands before its opening tag. Such markup can't
        // be written as XML, so reject it here (the caller retries) rather than at write time.
        if (tokens.any { it.original.startsWith("<") } && !isWellFormedFragment(result.toString())) return null

        // A resource value starting with @ or ? is read as a reference unless escaped.
        return if (result.startsWith("@") || result.startsWith("?")) "\\$result" else result.toString()
    }

    // "٣" (Arabic-Indic), "३" (Devanagari), … → 3
    private fun parseDigits(digits: String): Int? =
        digits.map { Character.digit(it, 10) }.takeIf { d -> d.none { it < 0 } }?.joinToString("")?.toIntOrNull()

    private fun isWellFormedFragment(innerXml: String): Boolean = try {
        DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .apply { setErrorHandler(null) }   // don't print parse errors to stderr
            .parse(ByteArrayInputStream("<r>$innerXml</r>".toByteArray(Charsets.UTF_8)))
        true
    } catch (e: Exception) {
        false
    }

    /** Text → what the translator should see: XML entities and Android backslash escapes removed. */
    private fun unescapeText(gap: String): String = gap
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&amp;", "&")
        // keep "\n" from being glued to the next word (restored by escapeText)
        .replace(Regex("""\\n(\S)"""), """\\n $1""")
        .replace("\\'", "'")
        .replace("\\\"", "\"")
        .replace("\\@", "@")
        .replace("\\?", "?")

    /** Inverse of [unescapeText]: translated text → inner-XML text with Android escapes. */
    private fun escapeText(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("'", "\\'")
        .replace("\"", "\\\"")
        .replace("\\ n", "\\n")

    private fun addTextGap(builder: Builder, gap: String) {
        if (gap.isEmpty()) return
        val text = unescapeText(gap)
        var last = 0
        for (match in placeholderRegex.findAll(text)) {
            builder.text(text.substring(last, match.range.first))
            builder.token(match.value)
            last = match.range.last + 1
        }
        builder.text(text.substring(last))
    }

    private class Builder(private val style: TokenStyle) {
        val out = StringBuilder()
        val tokens = mutableListOf<Token>()
        var hasLetters = false
            private set
        private var pendingAfter = -1

        fun text(s: String) {
            if (s.isEmpty()) return
            if (!hasLetters && s.replace("\\n", "").any { it.isLetter() }) hasLetters = true
            resolvePendingAfter(s[0])
            out.append(s)
        }

        fun token(original: String) {
            resolvePendingAfter('X')
            val padBefore = out.isNotEmpty() && !out.last().isWhitespace()
            if (padBefore) out.append(' ')
            val index = tokens.size
            tokens += Token(original, padBefore, padAfter = false)
            out.append(style.render(index))
            pendingAfter = index
        }

        // Decide whether the previous token needs a trailing space, now that the next char is known.
        private fun resolvePendingAfter(next: Char) {
            if (pendingAfter < 0) return
            if (!next.isWhitespace()) {
                out.append(' ')
                tokens[pendingAfter] = tokens[pendingAfter].copy(padAfter = true)
            }
            pendingAfter = -1
        }
    }
}
