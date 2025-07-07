package no.nav.helsearbeidsgiver.altinn

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.mockk.every
import kotlinx.coroutines.delay
import no.nav.helsearbeidsgiver.utils.cache.LocalCache
import no.nav.helsearbeidsgiver.utils.test.mock.mockStatic
import kotlin.time.Duration

fun mockAltinn3M2MClient(vararg responses: Pair<HttpStatusCode, String>): Altinn3M2MClient =
    mockClient(*responses) {
        Altinn3M2MClient("url", "4936", LocalCache.Config(Duration.ZERO, 1)) { "" }
    }

fun mockAltinn3OBOClient(vararg responses: Pair<HttpStatusCode, String>): Altinn3OBOClient =
    mockClient(*responses) {
        Altinn3OBOClient("url", "4936", LocalCache.Config(Duration.ZERO, 1))
    }

private fun <T : Any> mockClient(
    vararg responses: Pair<HttpStatusCode, String>,
    createClient: () -> T,
): T {
    val mockEngine = MockEngine.create {
        reuseHandlers = false
        requestHandlers.addAll(
            responses.map { (status, content) ->
                {
                    if (content == "timeout") {
                        delay(600)
                    }
                    respond(
                        content = content,
                        status = status,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }
            },
        )
    }

    val mockHttpClient = HttpClient(mockEngine) { configure() }

    return mockStatic(::createHttpClient) {
        every { createHttpClient() } returns mockHttpClient

        createClient()
    }
}
