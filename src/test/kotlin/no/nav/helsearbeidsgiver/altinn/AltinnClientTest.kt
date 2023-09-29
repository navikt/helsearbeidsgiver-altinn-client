package no.nav.helsearbeidsgiver.altinn

import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.row
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import io.ktor.client.plugins.ServerResponseException
import io.ktor.http.HttpStatusCode
import no.nav.helsearbeidsgiver.utils.test.resource.readResource

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
                withClue("$orgnr should yield $expected") {
                    altinnClient.harRettighetForOrganisasjon(IDENTITETSNUMMER, orgnr)
                        .shouldBe(expected)
                }
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
