package home_screen

import data.model.LanguageTemplate
import data.model.TranslationResult
import domain.model.LanguageModel

/** A discovered module shown in the UI with its base-string count and whether it's selected for translation. */
data class ModuleSelection(
    val name: String,
    val resPath: String,
    val stringCount: Int,
    val selected: Boolean = true,
)

data class HomeScreenState(
    val selectedLanguages: MutableSet<LanguageModel> = mutableSetOf(),
    val availableLanguages: List<LanguageModel> = emptyList(),
    val filteredList: List<LanguageModel> = emptyList(),
    val quickTranslate: Boolean = false,
    val translationResult: TranslationResult = TranslationResult.Idle,
    val showLoading: Boolean = false,
    val parallelTranslation: Boolean = true,
    val searchedText: String = "",
    val folderPath: String = "",
    val loadedPath: String = "",
    val modules: List<ModuleSelection> = emptyList(),
    val templates: List<LanguageTemplate> = emptyList(),

)