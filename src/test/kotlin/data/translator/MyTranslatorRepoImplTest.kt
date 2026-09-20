package data.translator

import data.network.NetworkResponse
import data.translator.api_interface.TranslatorApis
import domain.model.LanguageModel
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MyTranslatorRepoImplTest {

    private val spanish = LanguageModel("Spanish", "Español", "es")
    private val webOnly = LanguageModel("Acehnese", "Bahasa Acèh", "ace", onlyWebTranslate = true)

    /** Fake endpoint: records what it was sent and answers with [reply]. */
    private class FakeApi(val reply: (String) -> NetworkResponse<String>) : TranslatorApis {
        val received = mutableListOf<String>()
        override suspend fun getTranslation(fromLanguage: String?, toLanguage: String?, query: String?): NetworkResponse<String> {
            received += query.orEmpty()
            return reply(query.orEmpty())
        }
    }

    private fun ok(transform: (String) -> String) = FakeApi { NetworkResponse.Success(transform(it)) }
    private fun failing(msg: String = "boom") = FakeApi { NetworkResponse.Failure(msg) }

    private fun repo(api1: FakeApi, api2: FakeApi, api3: FakeApi) = MyTranslatorRepoImpl(api1, api2, api3)

    private fun translate(repo: MyTranslatorRepoImpl, value: String, lang: LanguageModel = spanish) =
        runBlocking { repo.getTranslation(toLanguage = lang, query = value) }

    @Test
    fun `translates and restores placeholders and markup`() {
        val api = ok { it.replace("Click", "Haga clic").replace("here", "aquí") }
        val result = translate(repo(failing(), api, failing()), "Click <b>here</b> for %1\$s")

        assertTrue(result is NetworkResponse.Success, result.error)
        assertEquals("Haga clic <b>aquí</b> for %1\$s", result.data)
    }

    @Test
    fun `sends plain text to the endpoint - no tags, no entities, no android escapes`() {
        val api = ok { it }
        translate(repo(failing(), api, failing()), "Don\\'t <b>stop</b> &amp; go")

        val sent = api.received.single()
        assertTrue('<' !in sent && "&amp;" !in sent && "\\'" !in sent, sent)
    }

    @Test
    fun `a damaged response is retried with the next token style`() {
        // Simulates Google deleting the "@n@" token (as it does for e.g. Tatar) but keeping "%n%".
        val api = FakeApi { sent ->
            if (sent.contains("@0@")) NetworkResponse.Success("Hola sin nada")
            else NetworkResponse.Success(sent.replace("Hello", "Hola"))
        }
        val result = translate(repo(failing(), api, failing()), "Hello %1\$s")

        assertTrue(result is NetworkResponse.Success, result.error)
        assertEquals("Hola %1\$s", result.data)
        assertEquals(2, api.received.size, "first style damaged, second style accepted")
        assertTrue(api.received[0].contains("@0@") && api.received[1].contains("%0%"), api.received.toString())
    }

    @Test
    fun `every token style is tried before giving up`() {
        val api = ok { "always drops the token" }
        val result = translate(repo(failing(), api, failing()), "Hello %1\$s")

        assertTrue(result is NetworkResponse.Failure)
        assertEquals(data.util.LocalizationUtils.TokenStyle.entries.size, api.received.size)
    }

    @Test
    fun `a failed request moves to the next endpoint without burning a token style`() {
        val good = ok { it.replace("Hello", "Hola") }
        val result = translate(repo(failing(), failing("down"), good), "Hello %1\$s")

        assertTrue(result is NetworkResponse.Success, result.error)
        assertTrue(good.received.single().contains("@0@"), "still the first style")
    }

    @Test
    fun `when every endpoint fails to answer each endpoint is asked exactly once`() {
        // A token style can't fix "Google isn't answering" (down, blocked, captcha). Retrying every style would
        // multiply the traffic sent to an endpoint that is already refusing us.
        val a1 = failing("down"); val a2 = failing("down"); val a3 = failing("down")
        val result = translate(repo(a1, a2, a3), "Hello %1\$s")

        assertTrue(result is NetworkResponse.Failure)
        // a normal language tries all three endpoints (api2, api3, then the scraper), once each
        assertEquals(listOf(1, 1, 1), listOf(a1.received.size, a2.received.size, a3.received.size))
    }

    @Test
    fun `a web-only language whose scraper is blocked sends one request, not one per token style`() {
        val scraper = failing("Api1 error: translation container not found")
        val result = translate(repo(scraper, failing(), failing()), "Hello %1\$s", webOnly)

        assertTrue(result is NetworkResponse.Failure)
        assertEquals(1, scraper.received.size)
    }

    @Test
    fun `fails when every endpoint returns a damaged or empty translation`() {
        val result = translate(repo(ok { "" }, ok { "no placeholder" }, ok { "  " }), "Hello %1\$s")

        assertTrue(result is NetworkResponse.Failure, "must not return a damaged string as success")
    }

    @Test
    fun `fails with the last error when every endpoint fails`() {
        val result = translate(repo(failing("a1"), failing("a2"), failing("a3")), "Hello")

        assertTrue(result is NetworkResponse.Failure)
        assertTrue(result.error!!.isNotBlank())
    }

    @Test
    fun `values with nothing to translate are returned verbatim without a network call`() {
        val api1 = ok { "SHOULD NOT BE USED" }
        val api2 = ok { "SHOULD NOT BE USED" }
        val api3 = ok { "SHOULD NOT BE USED" }

        listOf("%1\$s", "%1\$d / %2\$d", "12345", "@string/app_name", "").forEach { value ->
            val result = translate(repo(api1, api2, api3), value)
            assertTrue(result is NetworkResponse.Success, value)
            assertEquals(value, result.data)
        }
        assertTrue(api1.received.isEmpty() && api2.received.isEmpty() && api3.received.isEmpty())
    }

    @Test
    fun `web-only languages use the JSON endpoints first and the scraper is only the fallback`() {
        // The scraper is the endpoint Google captcha-blocks; the JSON endpoints translate these languages too.
        val scraper = ok { "scraper" }
        val json2 = ok { it.replace("Hello", "Hola") }
        val json3 = ok { "json3" }

        val result = translate(repo(scraper, json2, json3), "Hello", webOnly)

        assertTrue(result is NetworkResponse.Success, result.error)
        assertEquals("Hola", result.data)
        assertEquals(1, json2.received.size)
        assertTrue(scraper.received.isEmpty(), "scraper must not be touched while a JSON endpoint works")
    }

    @Test
    fun `a web-only language still falls back to the scraper when both JSON endpoints fail`() {
        val scraper = ok { it.replace("Hello", "Hola") }
        val result = translate(repo(scraper, failing("a"), failing("b")), "Hello", webOnly)

        assertTrue(result is NetworkResponse.Success, result.error)
        assertEquals("Hola", result.data)
        assertEquals(1, scraper.received.size)
    }

    @Test
    fun `the scraper is never part of the rotation while a JSON endpoint is healthy`() {
        val scraper = ok { "scraper" }
        val json2 = ok { it.replace("Hello", "Hola") }
        val json3 = ok { it.replace("Hello", "Hej") }
        val repo = repo(scraper, json2, json3)

        repeat(20) { translate(repo, "Hello $it") }

        assertTrue(scraper.received.isEmpty(), "scraper was asked ${scraper.received.size}x although both JSON endpoints answered")
        assertEquals(20, json2.received.size + json3.received.size)
        assertTrue(json2.received.isNotEmpty() && json3.received.isNotEmpty(), "the two JSON endpoints should share the load")
    }

    @Test
    fun `when one JSON endpoint is cooling the other one carries the traffic and the scraper stays untouched`() {
        now = 0
        val scraper = ok { "scraper" }
        val json2 = failing("HTTP 429 Too Many Requests")
        val json3 = ok { it.replace("Hello", "Hola") }
        val repo = coolingRepo(scraper, json2, json3)

        repeat(10) { translate(repo, "Hello $it") }

        assertEquals(1, json2.received.size, "one failed request, then it cools down")
        assertEquals(10, json3.received.size)
        assertTrue(scraper.received.isEmpty(), "scraper must stay untouched: ${scraper.received.size} requests")
    }

    // ---- endpoint cool-down ----------------------------------------------------------------------

    private var now = 0L
    private fun coolingRepo(a1: FakeApi, a2: FakeApi, a3: FakeApi) =
        MyTranslatorRepoImpl(a1, a2, a3, clock = { now }, cooldownMs = 1_000)

    @Test
    fun `an endpoint that just failed is not asked first again during its cool-down`() {
        now = 0
        val scraper = failing("captcha")           // slot 1 = the HTML scraper, tried last anyway
        val json2 = failing("boom")                 // fails once
        val json3 = ok { it.replace("Hello", "Hola") }
        val repo = coolingRepo(scraper, json2, json3)

        // first string: json2 fails, json3 answers
        assertTrue(translate(repo, "Hello one").data == "Hola one")
        assertEquals(1, json2.received.size)

        // next strings within the cool-down: json2 is not asked at all
        repeat(5) { translate(repo, "Hello again") }
        assertEquals(1, json2.received.size, "cooling endpoint must not be retried by the rotation")
        assertEquals(6, json3.received.size)
    }

    @Test
    fun `an endpoint is tried again once its cool-down has passed`() {
        now = 0
        var healthy = false
        val json2 = FakeApi { if (healthy) NetworkResponse.Success(it.replace("Hello", "Hola")) else NetworkResponse.Failure("down") }
        val json3 = ok { it.replace("Hello", "Hej") }
        val repo = coolingRepo(failing(), json2, json3)

        translate(repo, "Hello")                     // json2 fails -> cooling until t=1000
        healthy = true
        now = 500
        translate(repo, "Hello")                     // still cooling: not asked
        assertEquals(1, json2.received.size)

        now = 1_500                                    // cool-down over
        val result = translate(repo, "Hello")
        assertEquals(2, json2.received.size)
        assertEquals("Hola", result.data)
    }

    @Test
    fun `when every endpoint is cooling they are all still tried - no dead end`() {
        now = 0
        val a1 = failing("x"); val a2 = failing("y"); val a3 = failing("z")
        val repo = coolingRepo(a1, a2, a3)

        translate(repo, "Hello")                       // everything fails and starts cooling
        val again = translate(repo, "Hello")           // all cooling: must still ask them, not give up unasked

        assertTrue(again is NetworkResponse.Failure)
        assertEquals(listOf(2, 2, 2), listOf(a1.received.size, a2.received.size, a3.received.size))
    }

    @Test
    fun `translated text is re-escaped for android and xml`() {
        val api = ok { "No hagas eso & 'esto' \"aquello\" <mal>" }
        val result = translate(repo(failing(), api, failing()), "Don't")

        assertEquals("No hagas eso &amp; \\'esto\\' \\\"aquello\\\" &lt;mal&gt;", result.data)
    }
}
