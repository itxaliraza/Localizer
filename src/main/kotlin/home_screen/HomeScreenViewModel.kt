package home_screen

import data.FileXmlData
import data.FilesHelper
import data.FilesHelper.extractLanguageCode
import data.availableLanguages
import data.model.TranslationResult
import data.network.NetworkResponse
import data.translator.MyTranslatorRepoImpl
import data.util.ZipExtractor
import domain.model.LanguageModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeScreenViewModel(private val translatorRepoImpl: MyTranslatorRepoImpl) {
    private val _state = MutableStateFlow(HomeScreenState())
    val state = _state.asStateFlow()
    private val separator = "\u2023\u2023\u2023\u2023"
    private var extractedFiles: Map<String, String> = mapOf()
    private var translationJob: Job? = null

    init {
        _state.update {
            it.copy(
                availableLanguages = availableLanguages(),
                filteredList = availableLanguages()
            )
        }
    }

    fun updateSelectedLanguages(list: List<LanguageModel>) {
        _state.update {
            it.copy(selectedLanguages = list)
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

            state.copy(selectedLanguages = selectedList.toList())
        }
    }


    fun removeLanguage(langModel: LanguageModel) {
        val currentList = state.value.selectedLanguages.toMutableList()
        currentList.remove(langModel)
        _state.update {
            it.copy(selectedLanguages = currentList)
        }
    }

    fun dismissLoading() {
        _state.update {
            it.copy(showLoading = false)
        }
    }

    fun toggleParallel(checked: Boolean) {
        _state.update {
            it.copy(parallelTranslation = checked)
        }
    }


    fun extractZipFile(path: String?) {
        if (path == null) return
        CoroutineScope(Dispatchers.IO).launch {
            _state.update {
                it.copy(translationResult = TranslationResult.Idle)
            }
            if (path.endsWith("strings.xml")) {
                val content =
                    File(path).readText() // or use zis.readBytes().decodeToString() for a ZipInputStream

                extractedFiles = mapOf(
                    "values/strings.xml"
                            to
                            content
                )
            } else if (path.endsWith("zip", ignoreCase = true)) {
                extractedFiles =
                    ZipExtractor.extractZipFile(path)
                extractedFiles.forEach {
                    println("extracted zip =$it")
                }
                println("complete extracted zip =${extractedFiles.size}")

                val oldList = state.value.selectedLanguages.map { it.langCode }
                val selectedLanguages: List<LanguageModel> = extractedFiles.mapNotNull { entry ->
                    val code = extractLanguageCode(entry.key)
                    println("Lang code= $code")
                    val found = availableLanguages().firstOrNull {
                        (it.langCode == code || it.langCode == "en") && !oldList.contains(code)
                    }
                    if (found == null) {
                        println("Not found Lang code= $code")
                    }
                    found
                }
                println("selectedLanguages size= ${selectedLanguages.size}, actual= $selectedLanguages")


                val stateLanguages = state.value.selectedLanguages.toMutableList()
                stateLanguages.addAll(selectedLanguages)
                _state.update {
                    it.copy(selectedLanguages = stateLanguages)
                }
                println("current state size= ${state.value.selectedLanguages.size}, actual= ${state.value.selectedLanguages}")

            } else {
                _state.update {
                    it.copy(translationResult = TranslationResult.TranslationFailed(Exception("Not valid file")))
                }
            }
        }
    }

    fun translate() {
        translationJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val stateLangs = state.value.selectedLanguages.map { it.langCode }
                val downloadsPath = System.getProperty("user.home") + "/Downloads"
                val currentTime = SimpleDateFormat("dd_MMM", Locale.getDefault()).format(Date())
                val newFolderPath =
                    "$downloadsPath/translation_${currentTime}_${System.currentTimeMillis()}"
                val tempDir = File(newFolderPath)
                if (!tempDir.exists()) {
                    tempDir.mkdirs() // Create the folder if it doesn't exist
                }
                println("tempdir path = ${tempDir.path}")
                val filesXmlContent = FilesHelper.getFilesXmlContents(extractedFiles)


                val fileXmlDataMutableMap = filesXmlContent.filter { (key, _) ->
                    val langCode = extractLanguageCode(key)
                    langCode == "en" || stateLangs.contains(langCode)
                }.toMutableMap()
                println("fileXmlDataMutableMap= ${fileXmlDataMutableMap.keys}")
                val basePairs =
                    fileXmlDataMutableMap["values/strings.xml"]?.keyValuePairs ?: emptyMap()
//                println("basePairs = ${filesXmlContent["values/strings.xml"]}")
                println("filesXmlContent languages = ${fileXmlDataMutableMap.keys.size}, ${fileXmlDataMutableMap.keys}")


                val existingLanguages = fileXmlDataMutableMap.keys.mapNotNull { key ->
                    val code = extractLanguageCode(key)
                    if (code == "en" || stateLangs.contains(code).not())
                        null
                    else
                        code
                }.toSet()

                println("filesXmlContent existingLanguages = ${existingLanguages.size} $existingLanguages")

                val missingLanguages = state.value.selectedLanguages.map { it.langCode }
                    .toSet() - existingLanguages
                println("filesXmlContent missingLanguages = ${missingLanguages.size} $missingLanguages")

                println("Languages to translate = ${missingLanguages} + ${fileXmlDataMutableMap.keys}")
                println("Total Languages to translate = ${state.value.selectedLanguages.size}")

                val totalFilesToTranslate = state.value.selectedLanguages.size

                fileXmlDataMutableMap.onEachIndexed { index, entry ->
                    if (entry.key != "values/strings.xml") {
                        processFile(
                            Pair(entry.key, entry.value),
                            index,
                            totalFilesToTranslate,
                            basePairs,
                            tempDir
                        )
                    }
                }

                missingLanguages.forEachIndexed { index, language ->
                    val newFile = FileXmlData("<resources></resources>", emptyMap(), language)
                    println("filesXmlContent = ${(fileXmlDataMutableMap.size - 1)}")
//                    shouldParallelTasking = false
                    processFile(
                        Pair("values-$language/strings.xml", newFile),
                        (fileXmlDataMutableMap.size - 1) + index,
                        totalFilesToTranslate,
                        basePairs,
                        tempDir
                    )
                }
                _state.update {
                    it.copy(translationResult = TranslationResult.TranslationCompleted)
                }
                println("pathString path = ${tempDir.path}")
                val zipFilePath = "$downloadsPath/translation_comp.zip"
                FilesHelper.makeZipFile(
                    zipFilePath = zipFilePath,
                    tempDir = tempDir.path
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(translationResult = TranslationResult.TranslationFailed(e))
                }
            }
        }

    }

    private suspend fun processFile(
        entry: Pair<String, FileXmlData>,
        index: Int,
        totalSize: Int,
        basePairs: Map<String, String>,
        tempDir: File
    ) = withContext(Dispatchers.IO) {
        val file = entry.second
        _state.update {
            it.copy(
                translationResult = TranslationResult.UpdateProgress(
                    ((index.toFloat() / totalSize) * 100).toInt(),
                    file.languageCode
                )
            )
        }
        println("Progress  index= $index, actual = ${((index.toFloat() / totalSize) * 100).toInt()} ")
        val currentPairs = file.keyValuePairs
        val missingKeys = basePairs.filterKeys { it !in currentPairs }


        println("Translating ${file.languageCode}")
        val chunks = FilesHelper.combineStringsWithLimit(missingKeys.values.toList(), separator)
        val splittedResults = mutableListOf<List<String>>()

        chunks.forEach { chunk ->
            val result =
                translatorRepoImpl.getTranslation(query = chunk, toLanguage = file.languageCode)

            if (result is NetworkResponse.Success) {
                splittedResults.add(result.data?.split(separator) ?: listOf())
            } else {
                throw Exception(result.error)
            }
        }
        val flatSplittedResults = splittedResults.flatten()

        var translatedPairs: Map<String, String> = missingKeys.keys.zip(flatSplittedResults).toMap()
        println("Chunks  missingKeys= ${missingKeys.size} flatSplittedResults= $flatSplittedResults }")

        if (missingKeys.size == flatSplittedResults.size) {
            translatedPairs = missingKeys.keys.zip(flatSplittedResults).toMap()
            println("Chunk worked for entry ${entry.first}")
        } else {
            println("Chunk not worked for entry ${entry.first}")

            if (state.value.parallelTranslation) {
                val jobs: List<Deferred<Pair<String, String>>> = missingKeys.map { (key, value) ->
                    async {
                        val result = translatorRepoImpl.getTranslation(
                            query = value,
                            toLanguage = file.languageCode
                        )
                        if (result is NetworkResponse.Success) {
                            val translations = result.data?.replace("'", "\'") ?: value
                            key to translations
                        } else {
                            throw Exception(result.error)
                        }
                    }
                }
                translatedPairs = jobs.awaitAll().toMap()
            } else {
                translatedPairs = missingKeys.map { (key, value) ->
                    val result = translatorRepoImpl.getTranslation(
                        query = value,
                        toLanguage = file.languageCode
                    )
                    if (result is NetworkResponse.Success) {
                        val translations = result.data ?: value
                        key to translations
                    } else {
                        throw Exception(result.error)
                    }
                }.toMap()
            }

        }

        println("Chunk $translatedPairs")

        val finalContent = FilesHelper.addNewEntriesToXmlNew(currentPairs + translatedPairs)
        FilesHelper.writeXmlToFile(
            finalContent,
            "${tempDir.path}/values-${file.languageCode}/strings.xml"
        )


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

}