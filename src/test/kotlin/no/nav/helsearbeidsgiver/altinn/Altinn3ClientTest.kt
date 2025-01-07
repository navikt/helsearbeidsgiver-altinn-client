package no.nav.helsearbeidsgiver.altinn

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.row
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import no.nav.helsearbeidsgiver.utils.test.resource.readResource

private val validAltinnResponse = "rettighetene-til-tanja-minge.json".readResource()

private const val FNR = "01020354321"

class Altinn3ClientTest: StringSpec({
    "fnr har kun rettigheter tilknyttet organisasjoner som Altinn returnerer og som er underenheter" {
        val altinnClient = mockAltinn3Client(HttpStatusCode.OK, validAltinnResponse)

        listOf(
            row("810007842", true),
            row("810007702", false), // Er hovedenhet
            row("123456789", false), // Er ikke i listen fra responsen
        )
            .forEach { (orgnr, expected) ->
                withClue("$orgnr should yield $expected") {
                    altinnClient.harTilgangTilOrganisasjon(FNR, orgnr)
                        .shouldBe(expected)
                }
            }
    }

    "gyldig svar fra Altinn gir liste av organisasjoner" {
        val altinn3Client = mockAltinn3Client(HttpStatusCode.OK, validAltinnResponse)

        val authList = altinn3Client.hentTilganger(FNR)

        authList.size shouldBeExactly 3
    }
})
