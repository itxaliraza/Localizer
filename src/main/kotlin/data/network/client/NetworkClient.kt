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


    suspend inline fun makeStringNetworkRequest(
        url: String,
        requestType: RequestTypes,
        headers: Map<String, String>? = null,
    ): NetworkResponse<String> {
        return try {
//            println("hitting =${url}")
            val response: String = requestType.getHttpBuilder(url) {
                if (requestType is RequestTypes.Post) {
                    it.setBody(requestType.body)
                }
                headers?.let { headers ->
                    headers.forEach { (key, value) ->
                        it.header(key, value)
                    }
                }
             }.body()

            val newResponse: String = response
            (NetworkResponse.Success(newResponse))

        } catch (e: ClientRequestException) {
            (NetworkResponse.Failure(e.message ?: "Client request error"))
        } catch (e: ServerResponseException) {
            (NetworkResponse.Failure(e.message ?: "Server response error"))
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