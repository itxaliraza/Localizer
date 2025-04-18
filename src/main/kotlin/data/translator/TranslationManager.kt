package data.translator

import data.FileXmlData
import data.FilesHelper
import data.model.TranslationResult
import data.network.NetworkResponse
import domain.model.LanguageModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File


class TranslationManager(private val translatorRepoImpl: MyTranslatorRepoImpl) {
    private val separator = "\u2023\u2023\u2023\u2023"

    private var mParallelTranslation = true
    private var areBothChineseSelected = false
    fun translate(
        selectedLanguages: List<LanguageModel>,
        extractedFiles: Map<String, String>,
        outputDir: File,
        parallelTranslation: Boolean
    ): Flow<TranslationResult> = flow {
        mParallelTranslation = parallelTranslation
        try {

            val selectedLanguagesKeys = selectedLanguages.map { it.langCode }
            val filesXmlContent: Map<String, FileXmlData> = FilesHelper.getFilesXmlContents(extractedFiles)

            val transformedLangCodeMap: Map<String, FileXmlData> = filesXmlContent.map {
                it.value.languageCode to it.value
            }.toMap()

            if (selectedLanguagesKeys.contains("zh-CN") && selectedLanguagesKeys.contains("zh-TW")) {
                areBothChineseSelected = true
            }
            val basePairs =
                transformedLangCodeMap["en"]?.keyValuePairs ?: emptyMap()


            val totalFilesToTranslate = selectedLanguages.size
            selectedLanguages.forEachIndexed { index, lang ->
                if (lang.langCode != "en") {
                    emit(
                        TranslationResult.UpdateProgress(
                            ((index.toFloat() / selectedLanguages.size) * 100).toInt(),
                            lang.langCode
                        )
                    )
                    processTranslation(
                        transformedLangCodeMap[lang.langCode] ?: FileXmlData(
                            "<resources></resources>",
                            emptyMap(),
                            lang.langCode
                        ),
                        index,
                        totalFilesToTranslate,
                        basePairs,
                        outputDir,
                    )
                }
            }


            emit(TranslationResult.TranslationCompleted)

            println("pathString path = ${outputDir.path}")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(TranslationResult.TranslationFailed(e))
        }

    }.flowOn(Dispatchers.IO)


    private suspend fun processTranslation(
        file: FileXmlData,
        index: Int,
        totalSize: Int,
        basePairs: Map<String, String>,
        tempDir: File,
    ) = withContext(Dispatchers.IO) {


        println("Progress  index= $index, actual = ${((index.toFloat() / totalSize) * 100).toInt()} ")
        val currentPairs = file.keyValuePairs
        val missingKeys = basePairs.filterKeys { it !in currentPairs }


        val translatedPairs: Map<String, String>


        if (mParallelTranslation) {
            val jobs: List<Deferred<Pair<String, String>>> = missingKeys.map { (key, value) ->
                async {
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


        val finalContent = FilesHelper.addNewEntriesToXmlNew(currentPairs + translatedPairs)
        var modifiedCode = file.languageCode
        println("are both = $areBothChineseSelected    file code ${file.languageCode}")
        if (file.languageCode == "zh-CN" || file.languageCode == "zh-TW") {
            if (areBothChineseSelected) {
                modifiedCode = modifiedCode.replace("-", "-r")
            } else {
                modifiedCode = "zh"
            }
        }


        FilesHelper.writeXmlToFile(
            finalContent,
            "${tempDir.path}/values-${modifiedCode}/strings.xml"
        )


    }

}