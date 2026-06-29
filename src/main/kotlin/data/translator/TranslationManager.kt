package data.translator

import data.FileXmlData
import data.FilesHelper
import data.model.TranslationResult
import data.network.NetworkResponse
import data.util.ModuleExtraction
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
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicInteger


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
    /**
     * Translates every selected language across every discovered [modules] entry, module by module.
     * Each module is extracted independently and its translated `strings.xml` files are written back
     * into that module's own `res` folder. Progress is reported over the full (module × language)
     * work set so the bar advances smoothly across the whole project, not per module.
     */
    fun translate(
        selectedLanguages: List<LanguageModel>,
        modules: List<ModuleExtraction>,
        parallelTranslation: Boolean
    ): Flow<TranslationResult> = channelFlow {
        mParallelTranslation = parallelTranslation
        try {
            val langsToTranslate = selectedLanguages.filter { it.langCode != "en" }
            val totalUnits = (modules.size * langsToTranslate.size).coerceAtLeast(1)
            var completed = 0

            modules.forEach { module ->
                mChangeFileCodes = module.extraction.changeFileCodes
                val outputDir = File(module.resPath)

                val filesXmlContent: Map<String, FileXmlData> =
                    FilesHelper.getFilesXmlContents(module.extraction.extractedFiles)
                val transformedLangCodeMap: Map<String, FileXmlData> =
                    filesXmlContent.values.associateBy { it.languageCode }
                val basePairs = transformedLangCodeMap["en"]?.keyValuePairs ?: emptyMap()

                langsToTranslate.forEach { lang ->
                    // Capture the units-done count for this unit so per-string updates (emitted below
                    // as each key finishes) carry a stable overall "completedUnits / totalUnits" count.
                    val completedSoFar = completed
                    processTranslation(
                        lang,
                        transformedLangCodeMap[lang.langCode] ?: FileXmlData(
                            "<resources></resources>",
                            emptyMap(),
                            lang.langCode
                        ),
                        basePairs,
                        outputDir,
                        completedSoFar,
                        totalUnits,
                        module.moduleName,
                    )
                    completed++
                }
                println("Module '${module.moduleName}' done -> ${outputDir.path}")
            }

            send(TranslationResult.TranslationCompleted)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            send(TranslationResult.TranslationFailed(e))
        }

    }.flowOn(Dispatchers.IO)


    private suspend fun ProducerScope<TranslationResult>.processTranslation(
        lang: LanguageModel,
        file: FileXmlData,
        basePairs: Map<String, String>,
        tempDir: File,
        completedUnits: Int,
        totalUnits: Int,
        moduleName: String,
    ) = withContext(Dispatchers.IO) {

        val currentPairs = file.keyValuePairs
        val missingKeys = basePairs.filterKeys { it !in currentPairs }

        // Per-language string counter: emit an UpdateProgress every time a key finishes so the UI
        // can show "<done> / <total>" strings for the language currently being translated. The
        // counter is atomic because parallel mode increments it from concurrent coroutines, and
        // `send` on the channelFlow scope is safe to call from those coroutines.
        val total = missingKeys.size
        val done = AtomicInteger(0)
        suspend fun emitStringProgress() {
            send(
                TranslationResult.UpdateProgress(
                    lang.langCode, moduleName, completedUnits, totalUnits, done.get(), total
                )
            )
        }
        emitStringProgress()

        // Translate each missing key independently and tolerate per-key failures: a key that
        // can't be translated (all endpoints failed after retries) is skipped rather than
        // aborting the whole run. Skipped keys simply stay missing and are retried next run.
        val translatedPairs: Map<String, String> = if (mParallelTranslation) {
            val jobs: List<Deferred<Pair<String, String>?>> = missingKeys.map { (key, value) ->
                async {
                    translateKeyOrNull(lang, value, key).also {
                        done.incrementAndGet()
                        emitStringProgress()
                    }
                }
            }
            jobs.awaitAll().filterNotNull().toMap()
        } else {
            missingKeys.mapNotNull { (key, value) ->
                translateKeyOrNull(lang, value, key).also {
                    done.incrementAndGet()
                    emitStringProgress()
                }
            }.toMap()
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
            // Sanitize to a valid Android resource qualifier: e.g. pt-BR -> pt-rBR, zh-CN -> zh-rCN.
            val folderCode = FilesHelper.toAndroidResFolderCode(modifiedCode)

            FilesHelper.writeXmlToFile(
                finalContent,
                "${tempDir.path}/values-${folderCode}/strings.xml"
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