package home_screen

import data.model.TranslationResult
import domain.model.LanguageModel

data class HomeScreenState(
    val selectedLanguages: List<LanguageModel> = emptyList(),
    val availableLanguages: List<LanguageModel> = emptyList(),
    val filteredList: List<LanguageModel> = emptyList(),
    val quickTranslate: Boolean = false,
    val translationResult: TranslationResult = TranslationResult.Idle,
    val showLoading: Boolean = false,
    val parallelTranslation: Boolean = true,
    val searchedText: String = ""

)