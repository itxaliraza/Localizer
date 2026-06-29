package home_screen

import data.availableLanguages
import data.model.LanguageTemplate
import data.model.TranslationResult
import data.translator.TranslationManager
import data.util.FolderExtractor
import data.util.ModuleExtraction
import data.util.TemplatesRepository
import domain.model.LanguageModel
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HomeScreenViewModel(
    private val translationManager: TranslationManager,
    private val templatesRepository: TemplatesRepository,
) {
    private val _state = MutableStateFlow(HomeScreenState())
    val state = _state.asStateFlow()

    private val _oneTimeUiEvents:Channel<HomeScreenOneTimeEvents> = Channel()
    val oneTimeUiEvents = _oneTimeUiEvents.receiveAsFlow()

    private var modules: List<ModuleExtraction> = emptyList()
    private var translationJob: Job? = null

    init {
        _state.update {
            it.copy(
                availableLanguages = availableLanguages,
                filteredList = availableLanguages,
                templates = templatesRepository.load()
            )
        }
    }

    /**
     * Save the current language selection as a named, reusable template and persist it.
     * No-op when nothing is selected or [name] is blank. A fresh UUID is generated each call,
     * so saving twice with the same name yields two distinct templates (the user can delete one).
     */
    fun createTemplate(name: String) {
        val codes = state.value.selectedLanguages.map { it.langCode }
        if (codes.isEmpty() || name.isBlank()) return
        val template = LanguageTemplate(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            langCodes = codes
        )
        val updated = state.value.templates + template
        templatesRepository.save(updated)
        _state.update { it.copy(templates = updated) }
    }

    /**
     * Replace the current selection with exactly the languages in [template] (matched against
     * [HomeScreenState.availableLanguages] by code). Codes no longer in the list are silently
     * dropped. Replace — not merge — so applying a template is predictable ("switch to this set").
     */
    fun applyTemplate(template: LanguageTemplate) {
        val codeSet = template.langCodes.toSet()
        val matched = state.value.availableLanguages
            .filter { it.langCode in codeSet }
            .toMutableSet()
        _state.update { it.copy(selectedLanguages = matched) }
    }

    /** Delete the template with [id] and persist the change. */
    fun deleteTemplate(id: String) {
        val updated = state.value.templates.filterNot { it.id == id }
        templatesRepository.save(updated)
        _state.update { it.copy(templates = updated) }
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
            modules = FolderExtractor.extractModules(path.trim())

            if (modules.isEmpty()) {
                _state.update {
                    it.copy(
                        translationResult = TranslationResult.TranslationFailed(Exception("Not valid file")),
                        loadedPath = "",
                        modules = emptyList()
                    )
                }
                _oneTimeUiEvents.send(HomeScreenOneTimeEvents.FileLoadedFail)
            } else {
                // Pre-select every language already present on disk across all discovered modules.
                val discoveredLangs = modules.flatMap { it.extraction.selectedLangs }.distinct()
                println(discoveredLangs)
                val existingLangs = state.value.selectedLanguages.toMutableList()
                existingLangs.addAll(discoveredLangs)
                _state.update {
                    it.copy(
                        selectedLanguages = existingLangs.toMutableSet(),
                        loadedPath = path,
                        modules = modules.map { module ->
                            ModuleSelection(
                                name = module.moduleName,
                                resPath = module.resPath,
                                stringCount = module.baseStringCount,
                                selected = true
                            )
                        }
                    )
                }
                _oneTimeUiEvents.send(HomeScreenOneTimeEvents.FileLoadedSuccess)

            }
        }
    }


    fun toggleParallel(checked: Boolean) {
        _state.update {
            it.copy(parallelTranslation = checked)
        }
    }

    /**
     * Returns the base `values/strings.xml` text for the module at [resPath], for in-app preview.
     * Falls back to any extracted file if no base is present, or a placeholder if the module is gone.
     */
    fun moduleStringsXml(resPath: String): String {
        val module = modules.firstOrNull { it.resPath == resPath } ?: return ""
        return module.extraction.extractedFiles["values/strings.xml"]
            ?: module.extraction.extractedFiles.values.firstOrNull()
            ?: ""
    }

    /** Flip whether a discovered module (identified by its res path) is included in the next run. */
    fun toggleModule(resPath: String, selectAll: Boolean = false) {
        _state.update { state ->
            val modulesList = state.modules
            val updated = when {
                selectAll -> {
                    val allSelected = modulesList.all { it.selected }
                    modulesList.map { it.copy(selected = !allSelected) }
                }

                else -> modulesList.map {
                    if (it.resPath == resPath) it.copy(selected = !it.selected) else it
                }
            }
            state.copy(modules = updated)
        }
    }


    fun translate() {
        val selectedPaths = state.value.modules.filter { it.selected }.map { it.resPath }.toSet()
        val modulesToTranslate = modules.filter { it.resPath in selectedPaths }
        if (modulesToTranslate.isNotEmpty()) {
            translationJob = CoroutineScope(Dispatchers.IO).launch {
                translationManager.translate(
                    state.value.selectedLanguages.toList(),
                    modulesToTranslate,
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
        modules = emptyList()
        _state.update {
            it.copy(loadedPath = "", modules = emptyList())
        }
    }

}