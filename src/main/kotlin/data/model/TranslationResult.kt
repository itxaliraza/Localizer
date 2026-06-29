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
    data object TranslationCompleted:TranslationResult
    data class TranslationFailed(val exc:Exception):TranslationResult
}