package data.translator

import data.network.NetworkResponse
import data.translator.api_interface.TranslatorApis
import data.util.LocalizationUtils.TokenStyle
import data.util.LocalizationUtils.restoreAfterTranslation
import data.util.LocalizationUtils.sanitizeForTranslation
import domain.model.LanguageModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class MyTranslatorRepoImpl(
    private val translatorApi1Impl: TranslatorApis,
    private val translatorApi2Impl: TranslatorApis,
    private val translatorApi3Impl: TranslatorApis,
    // Injectable so tests don't sleep.
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val cooldownMs: Long = ENDPOINT_COOLDOWN_MS,
) : TranslationRepository {

    companion object {
        // An endpoint whose *request* just failed (down, rate-limited, captcha redirect) is tried last for this
        // long, instead of being retried first by the rotation for every string. Sending a third of all traffic
        // to an endpoint that is refusing us only prolongs a block.
        const val ENDPOINT_COOLDOWN_MS = 60_000L
    }

    private val coolUntil = ConcurrentHashMap<TranslatorApis, Long>()

    // Round-robin starting point for endpoint rotation. Instance state (Koin registers this repo
    // as `factory`) so it resets per translation session. The increment is a benign racy write
    // under parallel translation — it only nudges which endpoint is tried first.
    private var lastCalledIndex = 0

    override suspend fun getTranslation(
        fromLanguage: String,
        toLanguage: LanguageModel,
        query: String
    ): NetworkResponse<String> = withContext(Dispatchers.IO) {
        getTranslationResultOrFailure(
            fromLanguage = fromLanguage,
            toLanguage = toLanguage,
            query = query
        )
    }


    private suspend fun getTranslationResultOrFailure(
        fromLanguage: String,
        toLanguage: LanguageModel,
        query: String,
    ): NetworkResponse<String> {
        // Nothing to translate (only placeholders/digits/punctuation, or a `@string/x` reference):
        // keep the value verbatim instead of asking the translator to mangle it.
        if (!sanitizeForTranslation(query).translatable) return NetworkResponse.Success(query)

        var lastFailure: NetworkResponse<String> = NetworkResponse.Failure("No translation endpoint available")

        // No single token style survives every language: Google sometimes deletes a token that sits next to
        // a word it drops, and which token it deletes depends on the style. So a *damaged* response (token
        // lost/duplicated, malformed markup, blank) is retried with the next style. The endpoints share one
        // engine and return the same damage, so on damage we move to the next style rather than the next
        // endpoint; a failed *request* still moves to the next endpoint.
        for (style in TokenStyle.entries) {
            val sanitized = sanitizeForTranslation(query, style)
            var gotAnswer = false
            for ((index, translatorApi) in endpointsInOrder().withIndex()) {
                currentCoroutineContext().ensureActive()

                val translationResult = translatorApi.getTranslation(
                    fromLanguage = fromLanguage,
                    toLanguage = toLanguage.langCode,
                    query = sanitized.text
                )
                if (translationResult !is NetworkResponse.Success) {
                    coolUntil[translatorApi] = clock() + cooldownMs
                    lastFailure = translationResult
                    println("Trying translation api $index error ${lastFailure.error}")
                    continue
                }
                gotAnswer = true
                coolUntil.remove(translatorApi)
                val restored = translationResult.data?.let { restoreAfterTranslation(it, sanitized) }
                if (!restored.isNullOrBlank()) {
                    lastCalledIndex += 1
                    return NetworkResponse.Success(restored)
                }
                lastFailure = NetworkResponse.Failure(
                    "Endpoint returned an empty or damaged translation (placeholders/markup not preserved)"
                )
                println("Damaged translation with token style $style, trying the next style")
                break
            }
            // No endpoint answered at all (down, offline, or blocked by a captcha/"unusual traffic" page).
            // A different token style cannot help, and resending every style would only multiply the
            // traffic to an endpoint that is already refusing us.
            if (!gotAnswer) return lastFailure
        }
        return lastFailure
    }


    /**
     * Endpoints in the order to try them: the two JSON endpoints, rotated among the healthy ones, then the HTML
     * scraper. An endpoint that failed a request within [cooldownMs] is moved behind the healthy ones (still
     * tried as a last resort, so a wrongly-cooled endpoint can never cause a dead end).
     *
     * The scraper is **never part of the rotation**: it is the endpoint Google captcha-blocks, so it is only
     * asked when both JSON endpoints have just failed. (Rotating it among "healthy" endpoints sent it one
     * request in three every time its cool-down expired — extra traffic to a host that was blocking us.)
     *
     * Every language uses the same list. `LanguageModel.onlyWebTranslate` used to restrict a language to the
     * scraper, but the JSON endpoints translate 115 of those 116 languages (probed live). The manager's
     * "unchanged output" check catches the one that isn't supported.
     */
    private fun endpointsInOrder(): List<TranslatorApis> {
        val json = listOf(translatorApi2Impl, translatorApi3Impl)
        val now = clock()
        val (healthy, cooling) = json.partition { (coolUntil[it] ?: 0L) <= now }
        val start = if (healthy.isEmpty()) 0 else lastCalledIndex % healthy.size // circular rotation among the healthy ones
        return healthy.drop(start) + healthy.take(start) + cooling + translatorApi1Impl
    }


}
