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
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File


class TranslationManager(private val translatorRepoImpl: MyTranslatorRepoImpl) {
    private var mParallelTranslation = true
    private var mChangeFileCodes = mapOf<String, String>()

    // Cap simultaneous in-flight requests so we don't flood Google with hundreds of
    // concurrent calls (which triggers rate-limiting / 429s and cascades into failures).
    private val requestSemaphore = Semaphore(MAX_CONCURRENT_REQUESTS)

    companion object {
        private const val MAX_CONCURRENT_REQUESTS = 8
        private const val MAX_ATTEMPTS = 3
        private const val RETRY_BACKOFF_MS = 350L
    }
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

        // Translate each missing key independently and tolerate per-key failures: a key that
        // can't be translated (all endpoints failed after retries) is skipped rather than
        // aborting the whole run. Skipped keys simply stay missing and are retried next run.
        val translatedPairs: Map<String, String> = if (mParallelTranslation) {
            val jobs: List<Deferred<Pair<String, String>?>> = missingKeys.map { (key, value) ->
                async { translateKeyOrNull(lang, value, key) }
            }
            jobs.awaitAll().filterNotNull().toMap()
        } else {
            missingKeys.mapNotNull { (key, value) -> translateKeyOrNull(lang, value, key) }.toMap()
        }

        val skipped = missingKeys.size - translatedPairs.size
        if (skipped > 0) {
            println("Language ${lang.langCode}: $skipped/${missingKeys.size} keys failed and were skipped")
        }

        // Merge the freshly translated entries into the EXISTING target file so arrays, plurals,
        // comments and translatable=false strings are preserved. Even a partial result is written.
        if (translatedPairs.isNotEmpty()) {
            val finalContent = FilesHelper.mergeEntriesIntoXml(file.contents, translatedPairs)
            val modifiedCode = mChangeFileCodes[file.languageCode] ?: file.languageCode

            FilesHelper.writeXmlToFile(
                finalContent,
                "${tempDir.path}/values-${modifiedCode}/strings.xml"
            )
        }
    }

    /**
     * Translates a single key, returning `null` on failure instead of throwing. Bounded by
     * [requestSemaphore] to cap concurrency and retried up to [MAX_ATTEMPTS] with backoff to ride
     * out transient rate-limiting. User cancellation ([CancellationException]) still propagates.
     */
    private suspend fun translateKeyOrNull(
        lang: LanguageModel, value: String, key: String
    ): Pair<String, String>? = requestSemaphore.withPermit {
        repeat(MAX_ATTEMPTS) { attempt ->
            currentCoroutineContext().ensureActive()
            try {
                val result = translatorRepoImpl.getTranslation(query = value, toLanguage = lang)
                if (result is NetworkResponse.Success) {
                    return@withPermit key to (result.data ?: value)
                }
                println("Translate '$key' -> ${lang.langCode} attempt ${attempt + 1} failed: ${result.error}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                println("Translate '$key' -> ${lang.langCode} attempt ${attempt + 1} error: ${e.message}")
            }
            if (attempt < MAX_ATTEMPTS - 1) delay(RETRY_BACKOFF_MS * (attempt + 1))
        }
        null
    }

}