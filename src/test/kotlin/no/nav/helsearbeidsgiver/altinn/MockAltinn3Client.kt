package no.nav.helsearbeidsgiver.altinn

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.mockk.every
import no.nav.helsearbeidsgiver.utils.test.mock.mockStatic

fun mockAltinn3OBOClient(
    content: String = "",
    vararg statuses: HttpStatusCode,
): Altinn3OBOClient {
    val mockHttpClient =
        HttpClient(MockEngine) {
            engine {
                statuses.forEach { status ->
                    addHandler {
                        respond(
                            content = content,
                            status = status,
                            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                        )
                    }
                }
            }
            configure(3)
        }

    return mockStatic(::createHttpClient) {
        every { createHttpClient(any()) } returns mockHttpClient

        Altinn3OBOClient("url", "4936")
    }
}
