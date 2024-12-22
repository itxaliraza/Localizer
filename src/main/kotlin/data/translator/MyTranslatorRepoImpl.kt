package data.translator

import data.model.TranslationResult
import data.network.NetworkResponse
import data.translator.apis.TranslatorApi1Impl
import data.translator.apis.TranslatorApi2Impl
import data.translator.apis.TranslatorApi3Impl
import data.translator.apis.escapeApos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URLEncoder
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.pathString

var lastCalledIndex = 0

class MyTranslatorRepoImpl(
    private val translatorApi1Impl: TranslatorApi1Impl,
    private val translatorApi2Impl: TranslatorApi2Impl,
    private val translatorApi3Impl: TranslatorApi3Impl
) {


    suspend fun getTranslation(
        fromLanguage: String = "en",
        toLanguage: String,
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
        toLanguage: String,
        query: String,
    ): NetworkResponse<String> = withContext(Dispatchers.IO) {
        val translationApis = listOf(translatorApi2Impl, translatorApi3Impl, translatorApi1Impl)
        val totalApis = translationApis.size
        val lastIndex = lastCalledIndex
        for (index in 0 until totalApis) {
            val currentIndex = (lastIndex + index) % totalApis // Circular iterati             ensureActive()
            val translatorApi = translationApis[currentIndex]

            val translationResult = translatorApi.getTranslation(
                fromLanguage = fromLanguage,
                toLanguage = toLanguage,
                query = query
            )
            if (translationResult is NetworkResponse.Success) {
                lastCalledIndex += 1
                return@withContext NetworkResponse.Success(translationResult.data?.escapeApos() ?: "")
            }
            println("Trying translation api $index error ${translationResult.error}")

            if (index == translationApis.size - 1) {
                return@withContext translationResult
            }
        }
        return@withContext NetworkResponse.Failure("Something went wrong")

    }


}