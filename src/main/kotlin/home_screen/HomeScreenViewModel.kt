package home_screen

import data.availableLanguages
import data.model.LanguageTemplate
import data.model.TranslationResult
import data.translator.TranslationManager
import data.util.FolderExtractor
import data.util.LanguageCodeResolver
import data.util.LanguageListParser
import data.util.ModuleExtraction
import data.util.TemplatesRepository
import domain.model.LanguageImportResult
import domain.model.LanguageModel
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeScreenViewModel(
    private val translationManager: TranslationManager,
    private val templatesRepository: TemplatesRepository,
) {
    private val _state = MutableStateFlow(HomeScreenState())
    val state = _state.asStateFlow()

    private val _oneTimeUiEvents:Channel<HomeScreenOneTimeEvents> = Channel()
    val oneTimeUiEvents = _oneTimeUiEvents.receiveAsFlow()

    // One scope for the ViewModel's lifetime (was a fresh, unmanaged scope per call).
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var modules: List<ModuleExtraction> = emptyList()
    private var translationJob: Job? = null
    private var loadJob: Job? = null

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
     * Save [codes] (by default the current language selection) as a named, reusable template and persist it.
     * No-op when there are no codes or [name] is blank. A fresh UUID is generated each call,
     * so saving twice with the same name yields two distinct templates (the user can delete one).
     */
    fun createTemplate(name: String, codes: List<String> = state.value.selectedLanguages.map { it.langCode }) {
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
    fun applyTemplate(template: LanguageTemplate) = selectLanguagesByCode(template.langCodes)

    /**
     * Replace the current selection with the languages for [codes]. Each code is resolved through
     * [LanguageCodeResolver], so old spellings from saved templates (`id`, `pt`, `itg`) still select
     * their language after the list's codes change.
     */
    fun selectLanguagesByCode(codes: Collection<String>) {
        val matched = codes.mapNotNull { LanguageCodeResolver.resolve(it) }.toMutableSet()
        _state.update { it.copy(selectedLanguages = matched) }
    }

    /** Pick the supported languages out of a pasted or imported list (see [LanguageListParser]). */
    fun parseLanguageList(text: String): LanguageImportResult = LanguageListParser.parse(text)

    /**
     * Reads a user-chosen language list file as text, or null if it can't be read or is larger than
     * [MAX_IMPORT_BYTES] (a language list is tiny; anything bigger is almost certainly the wrong file).
     */
    fun readLanguageListFile(path: String): String? = runCatching {
        val file = File(path)
        if (!file.isFile || file.length() > MAX_IMPORT_BYTES) null else file.readText()
    }.getOrNull()

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
        // A newer load supersedes an older one still in flight, so a slow first load can't overwrite it.
        loadJob?.cancel()
        loadJob = scope.launch {
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
        if (translationJob?.isActive == true) return
        val selectedPaths = state.value.modules.filter { it.selected }.map { it.resPath }.toSet()
        val modulesToTranslate = modules.filter { it.resPath in selectedPaths }
        if (modulesToTranslate.isNotEmpty()) {
            translationJob = scope.launch {
                try {
                    translationManager.translate(
                        state.value.selectedLanguages.toList(),
                        modulesToTranslate,
                        state.value.parallelTranslation
                    ).collect { result ->
                        _state.update {
                            it.copy(translationResult = result)
                        }
                    }
                } finally {
                    // The run (finished, failed or cancelled) may have written files. Re-read them so the next
                    // run only translates what is still missing, instead of redoing everything from the
                    // stale snapshot taken at load time.
                    withContext(NonCancellable) { refreshModules() }
                }
            }
        }
    }

    /** Re-reads the loaded modules from disk without touching the module/language selection. */
    private suspend fun refreshModules() {
        val path = state.value.loadedPath
        if (path.isBlank()) return
        val fresh = FolderExtractor.extractModules(path.trim())
        if (fresh.isNotEmpty()) modules = fresh
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

    companion object {
        private const val MAX_IMPORT_BYTES = 512 * 1024
    }
}
