package domain.model

/**
 * Outcome of parsing a user-supplied language list (pasted text or a file).
 * [matched] keeps first-seen order with no duplicates; [unrecognized] holds code-like tokens
 * (`xx`, `pt-XX`) that match no supported language, so the user can see what was skipped.
 */
data class LanguageImportResult(
    val matched: List<LanguageModel>,
    val unrecognized: List<String>,
)
