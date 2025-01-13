package no.nav.helsearbeidsgiver.altinn

import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.row
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.plugins.ServerResponseException
import io.ktor.http.HttpStatusCode
import no.nav.helsearbeidsgiver.utils.test.resource.readResource

private val validAltinnResponse = "rettighetene-til-tanja-minge.json".readResource()

private const val FNR = "01020354321"

class Altinn3M2MClientTest :
    StringSpec({
        "fnr har kun rettigheter tilknyttet organisasjoner som Altinn returnerer og som er underenheter" {
            val altinn3Client = mockAltinn3M2MClient(content = validAltinnResponse, HttpStatusCode.OK)

            listOf(
                row("810007842", true),
                row("810007702", false), // Er hovedenhet
                row("123456789", false), // Er ikke i listen fra responsen
            ).forEach { (orgnr, expected) ->
                withClue("$orgnr should yield $expected") {
                    altinn3Client
                        .harTilgangTilOrganisasjon(FNR, orgnr)
                        .shouldBe(expected)
                }
            }
        }

        "gyldig svar fra Altinn gir liste av organisasjoner" {
            val altinn3Client = mockAltinn3M2MClient(content = validAltinnResponse, HttpStatusCode.OK)

            val authList = altinn3Client.hentTilganger(FNR)

            authList.size shouldBeExactly 3
        }

        "serverfeil trigger retry som gir gyldig svar" {
            val altinn3Client = mockAltinn3M2MClient(content = validAltinnResponse, HttpStatusCode.BadGateway, HttpStatusCode.OK)

            val tilganger = altinn3Client.hentTilganger(FNR)

            tilganger.size shouldBeExactly 3
        }

        "GatewayTimeout fra Altinn gir ServerResponseException" {
            val altinn3Client =
                mockAltinn3M2MClient(
                    content = validAltinnResponse,
                    HttpStatusCode.GatewayTimeout,
                )

            shouldThrowExactly<ServerResponseException> {
                altinn3Client.hentTilganger(FNR)
            }
        }

        "gyldig svar fra Altinn gir hierarki med liste av tilganger" {
            val altinn3Client = mockAltinn3M2MClient(content = validAltinnResponse, HttpStatusCode.OK)

            val tilgangRespons = altinn3Client.hentHierarkiMedTilganger(FNR)

            val hovedEnhet = tilgangRespons.hierarki.find { it.orgnr == "810007702" }
            hovedEnhet?.navn shouldBe "ANSTENDIG PIGGSVIN BYDEL"
            hovedEnhet?.underenheter shouldNotBe null
            hovedEnhet?.underenheter?.shouldHaveSize(3)
            hovedEnhet?.underenheter?.map {
                it.navn
            } shouldContainExactly setOf("ANSTENDIG PIGGSVIN BARNEHAGE", "ANSTENDIG PIGGSVIN SYKEHJEM", "ANSTENDIG PIGGSVIN BRANNVESEN")
            hovedEnhet?.underenheter?.map { it.orgnr } shouldContainExactly setOf("810007842", "810007982", "810008032")
        }
    })
