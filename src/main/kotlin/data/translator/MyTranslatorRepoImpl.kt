package data.translator

import data.network.NetworkResponse
import data.translator.apis.TranslatorApi1Impl
import data.translator.apis.TranslatorApi2Impl
import data.translator.apis.TranslatorApi3Impl
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
        val translationApis = listOf(translatorApi2Impl,  translatorApi3Impl,translatorApi1Impl,)

        translationApis.forEachIndexed { index, translatorApis ->
            println("Trying translation api ${index}")
            ensureActive()
            val translationResult = translatorApis.getTranslation(
                fromLanguage = fromLanguage,
                toLanguage = toLanguage,
                query = query
            )
            if (translationResult is NetworkResponse.Success) {
                println("Trying translation api $index success result =  ${translationResult.data}")
                return@withContext translationResult
            }
            println("Trying translation api $index error ${translationResult.error}")

            if (index == translationApis.size - 1) {
                return@withContext translationResult
            }
        }
        return@withContext NetworkResponse.Failure("Something went wrong")

    }


}