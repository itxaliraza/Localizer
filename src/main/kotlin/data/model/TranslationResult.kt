package data.model

sealed interface TranslationResult {
    data object Idle:TranslationResult
    data class UpdateProgress(
        val translatingLang:String,
        val moduleName:String="",
        // Overall, language-level progress shown as a count (e.g. "2/10"): how many (module × language)
        // units are fully done, and the total. The language currently being translated is `completedUnits + 1`.
        val completedUnits:Int=0,
        val totalUnits:Int=0,
        // Per-language string-level progress for the language currently being translated:
        // how many of this language's missing strings have finished so far, out of the total.
        val translatedStrings:Int=0,
        val totalStrings:Int=0
    ):TranslationResult

    /**
     * The run finished without being aborted. This does NOT mean everything was translated: keys that
     * failed on every endpoint are skipped, so [failedKeys] / [issues] must be surfaced to the user.
     * [issues] are human-readable, one per (module, language) unit that had a problem.
     */
    data class TranslationCompleted(
        val translatedKeys: Int = 0,
        val failedKeys: Int = 0,
        val issues: List<String> = emptyList(),
    ) : TranslationResult {
        val hasProblems: Boolean get() = failedKeys > 0 || issues.isNotEmpty()
    }

    data class TranslationFailed(val exc:Exception):TranslationResult
}
