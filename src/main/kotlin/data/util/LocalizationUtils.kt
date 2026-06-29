package data.util

object LocalizationUtils {

    private val placeholderRegex = Regex("%(\\d+)\\$([a-zA-Z])")
    private const val placeholderPrefix = "XXPH"
    private const val placeholderSuffix = "XX"

    fun sanitizeForTranslation(text: String): String {
        return text
            .replace(placeholderRegex) { match ->
                val index = match.groupValues[1]
                val type = match.groupValues[2].lowercase()
                "$placeholderPrefix${index}_${type}$placeholderSuffix"
            }
            .replace(Regex("""\\n(\S)"""), """\\n $1""")
            .replace("\\'", "'")
            .replace("\\\"", "\"")
    }

    fun restoreAfterTranslation(text: String): String {
        val flexiblePattern =
            Regex("(?i)$placeholderPrefix\\s*(\\d+)\\s*_\\s*([a-z])\\s*$placeholderSuffix")

        return text
            .replace(flexiblePattern) { match ->
                val index = match.groupValues[1]
                val type = match.groupValues[2].lowercase()
                "%$index\$$type"
            }
            // Android string resources require apostrophes and double quotes to be escaped.
            // All three endpoints now return plain text (API1 is HTML-unescaped at its layer),
            // so escape the real characters uniformly rather than the old `&quot;`-only hack.
            .replace("'", "\\'")
            .replace("\"", "\\\"")
            .replace("\\ n", "\\n")
    }
}
