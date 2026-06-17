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
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import no.nav.helsearbeidsgiver.utils.cache.LocalCache
import no.nav.helsearbeidsgiver.utils.test.mock.mockStatic
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

fun mockAltinn3M2MClient(
    vararg responses: Pair<HttpStatusCode, String>,
    scheduler: TestCoroutineScheduler? = null,
): Altinn3M2MClient =
    mockClient(responses.toList(), scheduler) {
        Altinn3M2MClient("url", Altinn3Ressurs.INNTEKTSMELDING, LocalCache.Config(Duration.ZERO, 1)) { "" }
    }

fun mockAltinn3OBOClient(
    vararg responses: Pair<HttpStatusCode, String>,
    scheduler: TestCoroutineScheduler? = null,
): Altinn3OBOClient =
    mockClient(responses.toList(), scheduler) {
        Altinn3OBOClient("url", Altinn3Ressurs.INNTEKTSMELDING, LocalCache.Config(Duration.ZERO, 1))
    }

private fun <T : Any> mockClient(
    responses: List<Pair<HttpStatusCode, String>>,
    scheduler: TestCoroutineScheduler? = null,
    createClient: () -> T,
): T {
    val mockEngine =
        MockEngine.create {
            if (scheduler != null) {
                // Unngår venting på delay-funksjon kallt i request handler
                dispatcher = StandardTestDispatcher(scheduler)
            }
            reuseHandlers = false
            requestHandlers.addAll(
                responses.map { (status, content) ->
                    {
                        if (content == "timeout") {
                            delay(3100.milliseconds)
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
