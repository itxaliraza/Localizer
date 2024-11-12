package data.translator.apis

 import data.network.client.NetworkClient
import data.network.client.RequestTypes
import data.network.NetworkResponse
import data.network.doIfSuccessOrFailure
 import data.translator.api_interface.TranslatorApis
 import kotlinx.coroutines.CancellationException
 import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder

class TranslatorApi1Impl :TranslatorApis{
    override suspend fun getTranslation(
        fromLanguage: String?,
        toLanguage: String?,
        query: String?
    ): NetworkResponse<String> = withContext(Dispatchers.IO) {
        try {
            val queryEnc = URLEncoder.encode(query, Charsets.UTF_8.toString())

            val translationUrl = String.format(
                "https://translate.google.com/m?sl=%s&tl=%s&q=%s",
                fromLanguage,
                toLanguage,
                queryEnc
            )
            val sb = java.lang.StringBuilder()
            val text = NetworkClient.makeStringNetworkRequest(
                url = translationUrl, requestType = RequestTypes.Get
            )
//
//            println("texttt=${text.data},${text.error}")
            text.doIfSuccessOrFailure(mCallback = {
                sb.append(it?.let { it1 -> getTranslationData(it1) })
                return@withContext (NetworkResponse.Success(sb.toString()))
            }, error = {
                return@withContext (NetworkResponse.Failure(it))
            })


        }
        catch (e: CancellationException) {
            throw e
        }catch (e: java.lang.Exception) {

            return@withContext NetworkResponse.Failure(e.message.toString())
        }

        return@withContext NetworkResponse.Failure("Something went wrong")


    }


    private fun getTranslationData(to_translate: String): String {
    println("api 1 getTranslationData $to_translate")
            var nativeText = "class=\"t0\">"
            val result =
                to_translate.substring(to_translate.indexOf(nativeText) + nativeText.length)
                    .split("<".toRegex()).toTypedArray()[0]
            return if (result == "html>") {
                nativeText = "class=\"result-container\">"
                to_translate.substring(to_translate.indexOf(nativeText) + nativeText.length)
                    .split("<".toRegex()).toTypedArray()[0] + ""
            } else {
                throw Exception("Api error")
            }

        return ""
    }
}