package data.translator.apis

import data.network.client.NetworkClient
import data.network.client.RequestTypes
import data.network.NetworkResponse
import data.network.doIfSuccessOrFailure
import data.translator.api_interface.TranslatorApis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.lang.StringBuilder
import java.net.URLEncoder
import kotlin.coroutines.cancellation.CancellationException

class TranslatorApi2Impl : TranslatorApis {
    override suspend fun getTranslation(
        fromLanguage: String?,
        toLanguage: String?,
        query: String?
    ): NetworkResponse<String> = withContext(Dispatchers.IO) {
        try {
            val queryEnc = URLEncoder.encode(query, Charsets.UTF_8.toString())

            val translationUrl = String.format(
                "https://translate.google.com/translate_a/single?&client=gtx&sl=%s&tl=%s&q=%s&dt=t",
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
            return@withContext NetworkResponse.Failure(e.message.toString())

        }
        return@withContext NetworkResponse.Failure("Something went wrong")


    }


    private fun getTranslationData(to_translate: String): String {
        val sb = StringBuilder()
        val jSONArray = JSONArray(to_translate).getJSONArray(0)
        for (i in 0 until jSONArray.length()) {
            val string = jSONArray.getJSONArray(i).getString(0)
            if (string.isNotEmpty() && string != "null") {
                sb.append(string)
            }
        }
        return sb.toString()
    }
}