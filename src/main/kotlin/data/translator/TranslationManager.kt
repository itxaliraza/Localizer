package data.translator

import data.FileXmlData
import data.FilesHelper
import data.model.TranslationResult
import data.network.NetworkResponse
import domain.model.LanguageModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File


class TranslationManager(private val translatorRepoImpl: MyTranslatorRepoImpl) {
    private var mParallelTranslation = true
    private var mChangeFileCodes = mapOf<String, String>()
    fun translate(
        selectedLanguages: List<LanguageModel>,
        extractedFiles: Map<String, String>,
        changeFileCodes: Map<String, String>,
        outputDir: File,
        parallelTranslation: Boolean
    ): Flow<TranslationResult> = flow {
        mParallelTranslation = parallelTranslation
        mChangeFileCodes = changeFileCodes
        try {
            val filesXmlContent: Map<String, FileXmlData> =
                FilesHelper.getFilesXmlContents(extractedFiles)

            val transformedLangCodeMap: Map<String, FileXmlData> = filesXmlContent.map {
                it.value.languageCode to it.value
            }.toMap()
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
                        lang,
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
        lang: LanguageModel,
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
                    getTranslation(lang, value, key)
                }
            }
            translatedPairs = jobs.awaitAll().toMap()
        } else {
            translatedPairs = missingKeys.map { (key, value) ->
                getTranslation(lang, value, key)
            }.toMap()
        }


        val finalContent = FilesHelper.addNewEntriesToXmlNew(currentPairs + translatedPairs)
        val modifiedCode = mChangeFileCodes[file.languageCode] ?: file.languageCode

        FilesHelper.writeXmlToFile(
            finalContent,
            "${tempDir.path}/values-${modifiedCode}/strings.xml"
        )


    }

    private suspend fun getTranslation(
        lang: LanguageModel, value: String, key: String
    ): Pair<String, String> {
        val result = translatorRepoImpl.getTranslation(
            query = value,
            toLanguage = lang
        )
        return if (result is NetworkResponse.Success) {
            val translations = result.data ?: value
            key to translations
        } else {
            throw Exception(result.error)
        }
    }

}