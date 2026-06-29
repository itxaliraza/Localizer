package data.translator

import data.network.NetworkResponse
import data.translator.apis.TranslatorApi1Impl
import data.translator.apis.TranslatorApi2Impl
import data.translator.apis.TranslatorApi3Impl
import data.util.LocalizationUtils.restoreAfterTranslation
import data.util.LocalizationUtils.sanitizeForTranslation
import domain.model.LanguageModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

class MyTranslatorRepoImpl(
    private val translatorApi1Impl: TranslatorApi1Impl,
    private val translatorApi2Impl: TranslatorApi2Impl,
    private val translatorApi3Impl: TranslatorApi3Impl
) {

    // Round-robin starting point for endpoint rotation. Instance state (Koin registers this repo
    // as `factory`) so it resets per translation session. The increment is a benign racy write
    // under parallel translation — it only nudges which endpoint is tried first.
    private var lastCalledIndex = 0

    suspend fun getTranslation(
        fromLanguage: String = "en",
        toLanguage: LanguageModel,
        query: String
    ): NetworkResponse<String> = withContext(Dispatchers.IO) {
        val result = getTranslationResultOrFailure(
            fromLanguage = fromLanguage,
            toLanguage = toLanguage,
            query = query
        )

        return@withContext result

    }


    private suspend fun getTranslationResultOrFailure(
        fromLanguage: String,
        toLanguage: LanguageModel,
        query: String,
    ): NetworkResponse<String> = withContext(Dispatchers.IO) {
        val translationApis = if (toLanguage.onlyWebTranslate) {
            listOf(translatorApi1Impl)
        } else {
            listOf(translatorApi2Impl, translatorApi3Impl, translatorApi1Impl)
        }
        val totalApis = translationApis.size
        val lastIndex = lastCalledIndex
        for (index in 0 until totalApis) {
            ensureActive()
            val currentIndex = (lastIndex + index) % totalApis // circular rotation
            val translatorApi = translationApis[currentIndex]


            val translationResult = translatorApi.getTranslation(
                fromLanguage = fromLanguage,
                toLanguage = toLanguage.langCode,
                query = sanitizeForTranslation(query)
            )
            if (translationResult is NetworkResponse.Success) {
                lastCalledIndex += 1
                return@withContext NetworkResponse.Success(
                    restoreAfterTranslation(translationResult.data ?: "")
                )
            }
            println("Trying translation api $index error ${translationResult.error}")

            if (index == translationApis.size - 1) {
                return@withContext translationResult
            }
        }
        return@withContext NetworkResponse.Failure("Something went wrong")

    }


}
