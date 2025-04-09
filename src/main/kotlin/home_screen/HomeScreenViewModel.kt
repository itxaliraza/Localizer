package home_screen

import data.availableLanguages
import data.model.TranslationResult
import data.util.ExtractionResult
import data.util.FolderExtractor
import domain.model.LanguageModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class HomeScreenViewModel(private val translationManager: TranslationManager) {
    private val _state = MutableStateFlow(HomeScreenState())
    val state = _state.asStateFlow()
    private var extractionResult: ExtractionResult? = null
    private var translationJob: Job? = null

    init {
        _state.update {
            it.copy(
                availableLanguages = availableLanguages(),
                filteredList = availableLanguages()
            )
        }
    }


    fun updateSelectedLanguages(model: LanguageModel?, selectAll: Boolean = false) {
        _state.update { state ->
            val selectedList = state.selectedLanguages.toMutableSet()
            when {
                selectAll -> {
                    selectedList.apply {
                        if (size == state.filteredList.size) clear()
                        else addAll(state.filteredList)
                    }
                }

                model != null -> {
                    if (!selectedList.add(model)) selectedList.remove(model)
                }
            }

            state.copy(selectedLanguages = selectedList.toMutableSet())
        }
    }


    fun loadFileFromPath(path: String) {

        _state.update {
            it.copy(translationResult = TranslationResult.Idle)
        }
        CoroutineScope(Dispatchers.IO).launch {
            extractionResult = FolderExtractor.getKeyWithStringsFromFolder(path.trim())

            if (extractionResult?.selectedLangs?.isEmpty() == true) {
                _state.update {
                    it.copy(translationResult = TranslationResult.TranslationFailed(Exception("Not valid file")), loadedPath = "")
                }
            } else {
                println(extractionResult!!.selectedLangs)
                val existingLangs = state.value.selectedLanguages.toMutableList()
                existingLangs.addAll(extractionResult!!.selectedLangs)
                _state.update {
                    it.copy(selectedLanguages = existingLangs.toMutableSet(), loadedPath = path)
                }
            }
        }
    }


    fun toggleParallel(checked: Boolean) {
        _state.update {
            it.copy(parallelTranslation = checked)
        }
    }


    fun translate() {
        println("tempdir path = ${state.value.folderPath}")
        if (extractionResult != null) {
            translationJob = CoroutineScope(Dispatchers.IO).launch {
                translationManager.translate(
                    state.value.selectedLanguages.toList(),
                    extractionResult!!.extractedFiles,
                    File(state.value.loadedPath.trim()),
                    state.value.parallelTranslation
                ).collectLatest { result ->
                    _state.update {
                        it.copy(translationResult = result)
                    }
                }
            }
        }
    }


    fun cancelTranslation() {
        translationJob?.cancel()
        _state.update {
            it.copy(translationResult = TranslationResult.TranslationFailed(Exception("Translation Cancelled")))
        }
    }

    fun searchLanguage(text: String) {
        val availableList = state.value.availableLanguages
        val filteredList = availableList.filter {
            it.langName.contains(text, true) || it.langCode.contains(text, true)
        }
        _state.update {
            it.copy(
                searchedText = text,
                filteredList = if (text.isEmpty()) availableList else filteredList
            )
        }
    }

    fun updateFolderPath(text: String) {

        _state.update {
            it.copy(
                folderPath = text,
            )
        }
    }

    fun clearLoadedFile() {
         _state.update {
             it.copy(loadedPath = "")
         }
    }

}