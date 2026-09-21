package data.network.client

import data.network.NetworkResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object NetworkClient {

    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            }
            )
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 90_000 // 10 seconds
            connectTimeoutMillis = 90_000 // 5 seconds
            socketTimeoutMillis = 90_000 // 15 seconds
        }
    }

    fun String.logable(): String {
        return this.replace("htt", "")
    }

    /** First ~80 characters of a response body on one line, for error messages. */
    fun String.snippet(max: Int = 80): String =
        replace(Regex("\\s+"), " ").trim().let { if (it.length > max) it.take(max) + "…" else it }


    suspend inline fun makeStringNetworkRequest(
        url: String,
        requestType: RequestTypes,
        headers: Map<String, String>? = null,
    ): NetworkResponse<String> {
        return try {
//            println("hitting =${url}")
            val httpResponse = requestType.getHttpBuilder(url) {
                if (requestType is RequestTypes.Post) {
                    it.setBody(requestType.body)
                }
                headers?.let { headers ->
                    headers.forEach { (key, value) ->
                        it.header(key, value)
                    }
                }
             }
            val response: String = httpResponse.body()

            // Ktor does not throw on 4xx/5xx here, so without this check a rate-limit or captcha page (HTTP 429)
            // is handed to the parsers as if it were a translation and fails with a confusing parse error.
            if (!httpResponse.status.isSuccess()) {
                NetworkResponse.Failure("HTTP ${httpResponse.status.value} ${httpResponse.status.description}: ${response.snippet()}")
            } else {
                NetworkResponse.Success(response)
            }

        } catch (e: ClientRequestException) {
            (NetworkResponse.Failure(e.message))
        } catch (e: ServerResponseException) {
            (NetworkResponse.Failure(e.message))
        } catch (e: Exception) {
            (NetworkResponse.Failure(e.message ?: "No Internet"))
        } finally {
        }
    }


    suspend fun RequestTypes.getHttpBuilder(
        url: String,
        callback: (HttpRequestBuilder) -> Unit
    ): HttpResponse {
        return when (this) {
            RequestTypes.Get -> {

                client.get(url) {

                    callback.invoke(this)
                }
            }

            is RequestTypes.Post -> {

                client.post(url) {
//                    println("Request ${this.body}")

                    callback.invoke(this)
                }
            }

        }
    }


}