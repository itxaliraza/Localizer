package home_screen

import data.FileXmlData
import data.FilesHelper
import data.FilesHelper.extractLanguageCode
import data.availableLanguages
import data.model.TranslationResult
import data.network.NetworkResponse
import data.translator.MyTranslatorRepoImpl
import data.util.ExtractionResult
import data.util.FolderExtractor
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeScreenViewModel(private val translationManager: TranslationManager) {
    private val _state = MutableStateFlow(HomeScreenState())
    val state = _state.asStateFlow()
    private var extractionResult:ExtractionResult?=null
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


    fun fileSelected(path:String?){
        if (path==null)
            return
        if (!path.endsWith("strings.xml") && !path.endsWith("zip", ignoreCase = true)) {
            _state.update {
                it.copy(translationResult = TranslationResult.TranslationFailed(Exception("Not valid file")))
            }
        }else{
            _state.update {
                it.copy(translationResult = TranslationResult.Idle)
            }
             CoroutineScope(Dispatchers.IO).launch {
                 extractionResult= ZipExtractor.extractZipFile(path)
                 val existingLangs=state.value.selectedLanguages.toMutableList()
                 existingLangs.addAll(extractionResult!!.selectedLangs)
                 _state.update {
                     it.copy(selectedLanguages = existingLangs.toMutableSet())
                 }
             }
        }
    }
    fun loadFileFromPath(path:String){

             _state.update {
                it.copy(translationResult = TranslationResult.Idle)
            }
            CoroutineScope(Dispatchers.IO).launch {
                extractionResult= FolderExtractor.getKeyWithStringsFromFolder(path.trim())

                if (extractionResult?.selectedLangs?.isEmpty()==true){
                    _state.update {
                        it.copy(translationResult = TranslationResult.TranslationFailed(Exception("Not valid file")))
                    }
                }else {
                    println(extractionResult!!.selectedLangs)
                    val existingLangs = state.value.selectedLanguages.toMutableList()
                    existingLangs.addAll(extractionResult!!.selectedLangs)
                    _state.update {
                        it.copy(selectedLanguages = existingLangs.toMutableSet())
                    }
                }
            }
    }
    fun removeLanguage(langModel: LanguageModel) {
        val currentList = state.value.selectedLanguages.toMutableList()
        currentList.remove(langModel)
        _state.update {
            it.copy(selectedLanguages = currentList.toMutableSet())
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




    fun translate(){
//        val downloadsPath = System.getProperty("user.home") + "/Downloads"
//        val currentTime = SimpleDateFormat("dd_MMM", Locale.getDefault()).format(Date())
//        val newFolderPath =
//            "$downloadsPath/translation_${currentTime}_${System.currentTimeMillis()}"
//        val tempDir = File(newFolderPath)
//        if (!tempDir.exists()) {
//            tempDir.mkdirs() // Create the folder if it doesn't exist
//        }
        println("tempdir path = ${state.value.folderPath}")
        if (extractionResult!=null){
           translationJob= CoroutineScope(Dispatchers.IO).launch {
                translationManager.translate(state.value.selectedLanguages.toList(),extractionResult!!.extractedFiles,File(state.value.folderPath.trim())).collectLatest {result->
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

}