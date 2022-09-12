package no.nav.helsearbeidsgiver.altinn

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.row
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.jackson

private const val IDENTITETSNUMMER = "01020354321"

private val validAltinnResponse = "organisasjoner-med-rettighet.json".readResource()

class AltinnClientTest : StringSpec({
    "id har kun rettigheter tilknyttet organisasjoner som Altinn returnerer og som er underenheter" {
        val altinnClient = mockAltinnClient(HttpStatusCode.OK, validAltinnResponse)

        listOf(
            row("910098896", true),
            row("910020102", false), // Mangler hovedenhet
            row("123456789", false) // Er ikke i listen fra responsen
        )
            .forEach { (orgnr, expected) ->
                altinnClient.harRettighetForOrganisasjon(IDENTITETSNUMMER, orgnr)
                    .shouldBe(expected)
            }
    }

    "gyldig svar fra Altinn gir liste av organisasjoner" {
        val altinnClient = mockAltinnClient(HttpStatusCode.OK, validAltinnResponse)

        val authList = altinnClient.hentRettighetOrganisasjoner(IDENTITETSNUMMER)

        authList.size shouldBeExactly 4
    }

    "GatewayTimeout fra Altinn gir ServerResponseException" {
        val altinnClient = mockAltinnClient(HttpStatusCode.GatewayTimeout)

        shouldThrowExactly<ServerResponseException> {
            altinnClient.hentRettighetOrganisasjoner(IDENTITETSNUMMER)
        }
    }

    "BadGateway fra Altinn gir ${AltinnBrukteForLangTidException::class.simpleName}" {
        val altinnClient = mockAltinnClient(HttpStatusCode.BadGateway)

        shouldThrowExactly<AltinnBrukteForLangTidException> {
            altinnClient.hentRettighetOrganisasjoner(IDENTITETSNUMMER)
        }
    }
})

private fun mockAltinnClient(status: HttpStatusCode, content: String = ""): AltinnClient {
    val mockEngine = MockEngine {
        respond(
            content = content,
            status = status,
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        )
    }

    val mockHttpClient = HttpClient(mockEngine) {
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
            }
        }
    }

    return AltinnClient(
        "url",
        "",
        "",
        "",
        mockHttpClient
    )
}

private fun String.readResource(): String =
    ClassLoader.getSystemResource(this).readText()
