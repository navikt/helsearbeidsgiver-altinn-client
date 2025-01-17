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

fun mockAltinnClient(
    status: HttpStatusCode,
    content: String = "",
): AltinnClient {
    val mockEngine =
        MockEngine {
            respond(
                content = content,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }

    val mockHttpClient = HttpClient(mockEngine) { configure(1) }

    return mockStatic(::createHttpClient) {
        every { createHttpClient(any()) } returns mockHttpClient
        AltinnClient("url", "", { "" }, "")
    }
}
