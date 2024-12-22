package home_screen

import data.model.TranslationResult
import domain.model.LanguageModel

data class HomeScreenState(
    val selectedLanguagesList: List<LanguageModel> = emptyList(),
    val quickTranslate: Boolean = false,
    val translationResult: TranslationResult = TranslationResult.Idle,
    val showLoading: Boolean = false,
    val parallelTranslation:Boolean=true
)