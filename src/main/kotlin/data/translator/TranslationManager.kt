package data.translator

import data.FileXmlData
import data.FilesHelper
import data.model.TranslationResult
import data.network.NetworkResponse
import data.util.LocalizationUtils
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


class TranslationManager(
    private val translatorRepo: TranslationRepository,
    // Linear back-off between retries of one string; a parameter so tests don't have to wait.
    private val retryBackoffMs: Long = RETRY_BACKOFF_MS,
) {

    // Cap simultaneous in-flight requests so we don't flood Google with hundreds of
    // concurrent calls (which triggers rate-limiting / 429s and cascades into failures).
    private val requestSemaphore = Semaphore(MAX_CONCURRENT_REQUESTS)

    companion object {
        private const val MAX_CONCURRENT_REQUESTS = 8
        private const val MAX_ATTEMPTS = 3
        const val RETRY_BACKOFF_MS = 350L

        // Google answers 200 with the source text for language codes it doesn't know. If at least this
        // many translatable strings all come back identical to the source, the language is treated as
        // unsupported rather than written out as "translated".
        private const val ECHO_CHECK_MIN_SAMPLE = 3

        // Circuit breaker. When Google is down or blocking us (offline, HTTP 429, a captcha "unusual traffic"
        // redirect) every request fails, and grinding through every string of every language only prolongs
        // a block. A language stops once this many strings have failed *after full retries* with no success at
        // all; the run stops once this many languages in a row did.
        private const val BREAKER_MIN_FAILED_KEYS = 6
        private const val BLOCKED_UNITS_BEFORE_ABORT = 3
    }

    /** Per-language success/failure tally that trips when the endpoints are evidently not answering. */
    private class Breaker {
        val successes = AtomicInteger(0)
        val failures = AtomicInteger(0)
        val open: Boolean get() = successes.get() == 0 && failures.get() >= BREAKER_MIN_FAILED_KEYS
    }

    /** What happened to one (module, language) unit. [issue] is set whenever something went wrong. */
    private data class UnitOutcome(
        val translated: Int,
        val failed: Int,
        val issue: String? = null,
        /** The breaker tripped: Google was not answering for this language (not a per-string problem). */
        val blocked: Boolean = false,
    )

    /**
     * Translates every selected language across every discovered [modules] entry, module by module.
     * Each module is extracted independently and its translated `strings.xml` files are written back
     * into that module's own `res` folder. Progress is reported over the full (module × language)
     * work set so the bar advances smoothly across the whole project, not per module.
     *
     * The terminal [TranslationResult.TranslationCompleted] carries how many strings were translated and
     * how many failed, plus one issue line per problematic unit — a run where nothing could be translated
     * (e.g. offline) is reported as such, not as a success.
     */
    fun translate(
        selectedLanguages: List<LanguageModel>,
        modules: List<ModuleExtraction>,
        parallelTranslation: Boolean
    ): Flow<TranslationResult> = channelFlow {
        try {
            val langsToTranslate = selectedLanguages.filter { it.langCode != "en" }
            val totalUnits = (modules.size * langsToTranslate.size).coerceAtLeast(1)
            var completed = 0
            var translatedTotal = 0
            var failedTotal = 0
            var blockedInARow = 0
            var stoppedEarly = false
            val issues = mutableListOf<String>()

            modules.forEach { module ->
                if (stoppedEarly) return@forEach
                val outputDir = File(module.resPath)

                val filesXmlContent: Map<String, FileXmlData> =
                    FilesHelper.getFilesXmlContents(module.extraction.extractedFiles)
                val transformedLangCodeMap: Map<String, FileXmlData> =
                    filesXmlContent.values.associateBy { it.languageCode }
                val basePairs = transformedLangCodeMap["en"]?.keyValuePairs ?: emptyMap()

                langsToTranslate.forEach { lang ->
                    if (stoppedEarly) return@forEach
                    // Capture the units-done count for this unit so per-string updates (emitted below
                    // as each key finishes) carry a stable overall "completedUnits / totalUnits" count.
                    val completedSoFar = completed
                    val outcome = processTranslation(
                        lang,
                        transformedLangCodeMap[lang.langCode] ?: FileXmlData(
                            FilesHelper.EMPTY_STRINGS_XML,
                            emptyMap(),
                            lang.langCode
                        ),
                        basePairs,
                        outputDir,
                        module.extraction.changeFileCodes,
                        parallelTranslation,
                        completedSoFar,
                        totalUnits,
                        module.moduleName,
                    )
                    translatedTotal += outcome.translated
                    failedTotal += outcome.failed
                    outcome.issue?.let { issues += "${module.moduleName} → ${lang.langCode}: $it" }
                    completed++

                    blockedInARow = if (outcome.blocked) blockedInARow + 1 else 0
                    if (blockedInARow >= BLOCKED_UNITS_BEFORE_ABORT) {
                        stoppedEarly = true
                        issues += "Stopped early: $BLOCKED_UNITS_BEFORE_ABORT languages in a row could not reach Google. " +
                                "It is offline or rate-limiting this machine (a captcha / \"unusual traffic\" block usually " +
                                "clears within a few hours). Nothing is lost — press Retry later."
                    }
                }
                println("Module '${module.moduleName}' done -> ${outputDir.path}")
            }

            send(TranslationResult.TranslationCompleted(translatedTotal, failedTotal, issues))
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
        changeFileCodes: Map<String, String>,
        parallelTranslation: Boolean,
        completedUnits: Int,
        totalUnits: Int,
        moduleName: String,
    ): UnitOutcome = withContext(Dispatchers.IO) {

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
        if (missingKeys.isEmpty()) return@withContext UnitOutcome(translated = 0, failed = 0)
        val breaker = Breaker()

        // Translate each missing key independently and tolerate per-key failures: a key that
        // can't be translated (all endpoints failed after retries) is skipped rather than
        // aborting the whole run. Skipped keys stay missing and are retried next run.
        val translatedPairs: Map<String, String> = if (parallelTranslation) {
            val jobs: List<Deferred<Pair<String, String>?>> = missingKeys.map { (key, value) ->
                async {
                    translateKeyOrNull(lang, value, key, breaker).also {
                        done.incrementAndGet()
                        emitStringProgress()
                    }
                }
            }
            jobs.awaitAll().filterNotNull().toMap()
        } else {
            missingKeys.mapNotNull { (key, value) ->
                translateKeyOrNull(lang, value, key, breaker).also {
                    done.incrementAndGet()
                    emitStringProgress()
                }
            }.toMap()
        }

        // Array/plurals items are all-or-nothing: if one item of a group failed, drop the whole group so
        // we never write an array with a missing item (which would shift every later index).
        val incompleteGroups = missingKeys.keys.filter { it !in translatedPairs }
            .mapNotNull { FilesHelper.groupOf(it) }.toSet()
        val usable = translatedPairs.filterKeys { FilesHelper.groupOf(it) !in incompleteGroups }
        val failed = total - usable.size

        if (usable.isEmpty()) {
            println("Language ${lang.langCode}: all $total keys failed")
            return@withContext if (breaker.open) {
                UnitOutcome(
                    0, failed,
                    "Google is not answering (offline, or rate-limited) — stopped after ${breaker.failures.get()} failed strings",
                    blocked = true,
                )
            } else {
                UnitOutcome(0, failed, "no strings could be translated (check your internet connection)")
            }
        }

        // Unsupported language codes get a 200 with the source text back. Don't write that as a translation.
        val prose = usable.filter { (key, _) -> LocalizationUtils.hasTranslatableText(missingKeys.getValue(key)) }
        if (prose.size >= ECHO_CHECK_MIN_SAMPLE && prose.all { (key, value) -> value == missingKeys[key] }) {
            println("Language ${lang.langCode}: translator returned the source text unchanged")
            return@withContext UnitOutcome(
                0, total, "translator returned the source text unchanged — language may not be supported"
            )
        }

        // Merge the freshly translated entries into the EXISTING target file so arrays, plurals,
        // comments and translatable=false strings are preserved. Even a partial result is written.
        try {
            val finalContent = FilesHelper.mergeEntriesIntoXml(file.contents, usable)
            val modifiedCode = changeFileCodes[file.languageCode] ?: file.languageCode
            // Sanitize to a valid Android resource qualifier: e.g. pt-BR -> pt-rBR, zh-CN -> zh-rCN.
            val folderCode = FilesHelper.toAndroidResFolderCode(modifiedCode)

            FilesHelper.writeXmlToFile(
                finalContent,
                "${tempDir.path}/values-${folderCode}/strings.xml"
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            println("Language ${lang.langCode}: write failed: ${e.message}")
            return@withContext UnitOutcome(0, total, "could not write strings.xml: ${e.message}")
        }

        UnitOutcome(
            translated = usable.size,
            failed = failed,
            issue = if (failed > 0) "$failed of $total strings could not be translated" else null
        )
    }

    /**
     * Translates a single key, returning `null` on failure instead of throwing. Bounded by
     * [requestSemaphore] to cap concurrency and retried up to [MAX_ATTEMPTS] with backoff to ride
     * out transient rate-limiting. User cancellation ([CancellationException]) still propagates.
     */
    private suspend fun translateKeyOrNull(
        lang: LanguageModel, value: String, key: String, breaker: Breaker
    ): Pair<String, String>? = requestSemaphore.withPermit {
        if (breaker.open) return@withPermit null   // endpoints aren't answering: don't add to the traffic
        repeat(MAX_ATTEMPTS) { attempt ->
            currentCoroutineContext().ensureActive()
            if (breaker.open) return@withPermit null
            try {
                val result = translatorRepo.getTranslation(toLanguage = lang, query = value)
                val data = result.data
                if (result is NetworkResponse.Success && data != null) {
                    breaker.successes.incrementAndGet()
                    return@withPermit key to data
                }
                println("Translate '$key' -> ${lang.langCode} attempt ${attempt + 1} failed: ${result.error}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                println("Translate '$key' -> ${lang.langCode} attempt ${attempt + 1} error: ${e.message}")
            }
            if (attempt < MAX_ATTEMPTS - 1) delay(retryBackoffMs * (attempt + 1))
        }
        breaker.failures.incrementAndGet()
        null
    }

}
