package data.model

sealed interface TranslationResult {
    data object Idle:TranslationResult
    data class UpdateProgress(val progress:Int,val translatingLang:String):TranslationResult
    data object TranslationCompleted:TranslationResult
    data class TranslationFailed(val exc:Exception):TranslationResult
}