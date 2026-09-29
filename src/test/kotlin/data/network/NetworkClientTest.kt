package data.network

import com.sun.net.httpserver.HttpServer
import data.network.client.NetworkClient
import data.network.client.NetworkClient.snippet
import data.network.client.RequestTypes
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NetworkClientTest {

    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply { start() }
    private val base get() = "http://127.0.0.1:${server.address.port}"

    @AfterTest
    fun stop() = server.stop(0)

    private fun respond(path: String, status: Int, body: String) {
        server.createContext(path) { ex ->
            val bytes = body.toByteArray()
            ex.responseHeaders.add("Content-Type", "text/html; charset=utf-8")
            ex.sendResponseHeaders(status, bytes.size.toLong())
            ex.responseBody.use { it.write(bytes) }
        }
    }

    private fun get(path: String) = runBlocking {
        NetworkClient.makeStringNetworkRequest(url = base + path, requestType = RequestTypes.Get)
    }

    @Test
    fun `a 200 response is returned as success with its body`() {
        respond("/ok", 200, "[[\"Hola\"]]")
        val result = get("/ok")

        assertTrue(result is NetworkResponse.Success)
        assertEquals("[[\"Hola\"]]", result.data)
    }

    @Test
    fun `a rate-limit or block page is a failure that names the status - not success text for the parser`() {
        respond("/blocked", 429, "<html><body>  Our systems have detected\n unusual traffic </body></html>")
        val result = get("/blocked")

        assertTrue(result is NetworkResponse.Failure, "a 429 page must not be handed on as a translation")
        assertTrue(result.error!!.startsWith("HTTP 429"), result.error)
        assertTrue("unusual traffic" in result.error!!, "the body snippet helps diagnose: ${result.error}")
    }

    @Test
    fun `a server error is a failure too`() {
        respond("/boom", 503, "try later")
        val result = get("/boom")

        assertTrue(result is NetworkResponse.Failure)
        assertTrue(result.error!!.startsWith("HTTP 503"), result.error)
    }

    @Test
    fun `snippet collapses whitespace and caps the length`() {
        assertEquals("a b c", "  a \n b\t c ".snippet())
        val long = "x".repeat(200).snippet(80)
        assertEquals(81, long.length) // 80 chars + the ellipsis
        assertTrue(long.endsWith("…"))
    }
}
