package data.translator

import data.network.NetworkResponse
import data.translator.apis.TranslatorApi1Impl
import data.translator.apis.TranslatorApi2Impl
import data.translator.apis.TranslatorApi3Impl
import data.util.LocalizationUtils.restoreAfterTranslation
import data.util.LocalizationUtils.sanitizeForTranslation
import domain.model.LanguageModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

var lastCalledIndex = 0

class MyTranslatorRepoImpl(
    private val translatorApi1Impl: TranslatorApi1Impl,
    private val translatorApi2Impl: TranslatorApi2Impl,
    private val translatorApi3Impl: TranslatorApi3Impl
) {

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
            val currentIndex =
                (lastIndex + index) % totalApis // Circular iterati             ensureActive()
            val translatorApi = translationApis[currentIndex]


            val translationResult = translatorApi.getTranslation(
                fromLanguage = fromLanguage,
                toLanguage = toLanguage.langCode,
                query = sanitizeForTranslation(query)
            )
            if (translationResult is NetworkResponse.Success) {
                lastCalledIndex += 1
//                if (query.contains("&") || query.contains("'") || query.contains("\'")) {
                if (query.contains("\"")) {
                    println("result query = $query")
                    println("result query sanitize ${sanitizeForTranslation(query)}")
                    println("result simple response ${toLanguage.langCode} ${translationResult.data}")
                    println(
                        "result after restore ${
                            restoreAfterTranslation(translationResult.data ?: "")
                        }"
                    )
                }
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
