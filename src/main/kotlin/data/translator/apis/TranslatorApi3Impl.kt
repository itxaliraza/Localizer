package data.translator.apis

import data.network.client.NetworkClient
import data.network.client.RequestTypes
import data.network.NetworkResponse
import data.network.doIfSuccessOrFailure
import data.translator.api_interface.TranslatorApis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.URLEncoder
import kotlin.coroutines.cancellation.CancellationException

class TranslatorApi3Impl : TranslatorApis {
    override suspend fun getTranslation(
        fromLanguage: String?,
        toLanguage: String?,
        query: String?
    ): NetworkResponse<String> = withContext(Dispatchers.IO) {
        try {
            val queryEnc = URLEncoder.encode(query, Charsets.UTF_8.toString())

            val translationUrl = String.format(
                "https://clients4.google.com/translate_a/t?client=dict-chrome-ex&sl=%s&tl=%s&q=%s&dt=t",
                fromLanguage,
                toLanguage,
                queryEnc
            )
            val text = NetworkClient.makeStringNetworkRequest(
                url = translationUrl, requestType = RequestTypes.Get
            )
//
            println("texttt=${text.data},${text.error}")
            text.doIfSuccessOrFailure(mCallback = {
                val result = getTranslationData(it!!)
                return@withContext (NetworkResponse.Success(result))
            }, error = {
                return@withContext (NetworkResponse.Failure(it))
            })

        } catch (e: CancellationException) {
            throw e
        } catch (e: java.lang.Exception) {
            return@withContext (NetworkResponse.Failure(e.message.toString()))

        }

        return@withContext NetworkResponse.Failure("Something went wrong")


    }


    private fun getTranslationData(to_translate: String): String {
        try {
            // Parse the JSON array string
            val jsonArray = JSONArray(to_translate)

            // Get the value at the specified index
            return jsonArray.optString(0)
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
}