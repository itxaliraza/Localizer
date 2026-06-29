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


    private fun getTranslationData(toTranslate: String): String {
        // The mobile page wraps the translation in a single container. Current markup uses
        // `class="result-container">`; older markup used `class="t0">`. Try both explicitly
        // instead of relying on byte-offset coincidences in the HTML preamble.
        val marker = listOf("class=\"result-container\">", "class=\"t0\">")
            .firstOrNull { toTranslate.contains(it) }
            ?: throw Exception("Api1 error: translation container not found")

        val start = toTranslate.indexOf(marker) + marker.length
        val raw = toTranslate.substring(start).substringBefore("<")
        return raw.unescapeHtml()
    }

    // The container text is HTML-escaped (e.g. `&quot;`, `&amp;`). Decode it so the value we hand
    // back is the same plain text the JSON endpoints return — otherwise `&` would later be
    // double-escaped to `&amp;amp;` when written into the XML.
    private fun String.unescapeHtml(): String =
        replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&#x27;", "'")
            .replace("&amp;", "&")
}


