package no.nav.helsearbeidsgiver.altinn

import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.row
import io.kotest.datatest.withData
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

class Altinn3ClientTest :
    FunSpec({
        context("hent tilganger vellykket") {
            withData(
                mapOf(
                    "svar fra Altinn M2M gir liste av organisasjoner" to mockAltinn3M2MClient(content = validAltinnResponse, HttpStatusCode.OK),
                    "svar fra Altinn OBO gir liste av organisasjoner" to mockAltinn3OBOClient(content = validAltinnResponse, HttpStatusCode.OK),
                    "serverfeil fra Altinn M2M trigger retry som gir gyldig svar" to
                        mockAltinn3M2MClient(content = validAltinnResponse, HttpStatusCode.BadGateway, HttpStatusCode.OK),
                    "serverfeil fra Altinn OBO trigger retry som gir gyldig svar" to
                        mockAltinn3OBOClient(content = validAltinnResponse, HttpStatusCode.BadGateway, HttpStatusCode.OK),
                ),
            ) { altinn3Client ->
                val tilganger =
                    when (altinn3Client) {
                        is Altinn3M2MClient -> altinn3Client.hentTilganger(FNR)
                        is Altinn3OBOClient -> altinn3Client.hentTilganger(FNR, { "" })
                        else -> throw IllegalStateException("Altinn3Client må være av typen Altinn3M2MClient eller Altinn3OBOClient")
                    }
                tilganger.size shouldBeExactly 3
            }
        }

        context("fnr har kun rettigheter tilknyttet organisasjoner som Altinn returnerer og som er underenheter") {
            withData(
                mapOf(
                    "for Altinn M2M" to mockAltinn3M2MClient(content = validAltinnResponse, HttpStatusCode.OK),
                    "for Altinn OBO" to mockAltinn3OBOClient(content = validAltinnResponse, HttpStatusCode.OK),
                ),
            ) { altinn3Client ->

                listOf(
                    row("810007842", true),
                    row("810007702", false), // Er hovedenhet
                    row("123456789", false), // Er ikke i listen fra responsen
                ).forEach { (orgnr, expected) ->
                    val harTilgang =
                        when (altinn3Client) {
                            is Altinn3M2MClient -> altinn3Client.harTilgangTilOrganisasjon(FNR, orgnr)
                            is Altinn3OBOClient -> altinn3Client.harTilgangTilOrganisasjon(FNR, orgnr, { "" })
                            else -> throw IllegalStateException("Altinn3Client må være av typen Altinn3M2MClient eller Altinn3OBOClient")
                        }

                    withClue("$orgnr should yield $expected") {
                        harTilgang shouldBe expected
                    }
                }
            }
        }

        context("gyldig svar fra Altinn gir hierarki med liste av tilganger") {
            withData(
                mapOf(
                    "for Altinn M2M" to mockAltinn3M2MClient(content = validAltinnResponse, HttpStatusCode.OK),
                    "for Altinn OBO" to mockAltinn3OBOClient(content = validAltinnResponse, HttpStatusCode.OK),
                ),
            ) { altinn3Client ->
                val tilgangRespons =
                    when (altinn3Client) {
                        is Altinn3M2MClient -> altinn3Client.hentHierarkiMedTilganger(FNR)
                        is Altinn3OBOClient -> altinn3Client.hentHierarkiMedTilganger(FNR, { "" })
                        else -> throw IllegalStateException("Altinn3Client må være av typen Altinn3M2MClient eller Altinn3OBOClient")
                    }

                val hovedEnhet = tilgangRespons.hierarki.find { it.orgnr == "810007702" }
                hovedEnhet?.navn shouldBe "ANSTENDIG PIGGSVIN BYDEL"
                hovedEnhet?.underenheter shouldNotBe null
                hovedEnhet?.underenheter?.shouldHaveSize(3)
                hovedEnhet?.underenheter?.map {
                    it.navn
                } shouldContainExactly setOf("ANSTENDIG PIGGSVIN BARNEHAGE", "ANSTENDIG PIGGSVIN SYKEHJEM", "ANSTENDIG PIGGSVIN BRANNVESEN")
                hovedEnhet?.underenheter?.map { it.orgnr } shouldContainExactly setOf("810007842", "810007982", "810008032")
            }
        }

        context("kast server exception") {
            withData(
                mapOf(
                    "GatewayTimeout fra Altinn M2M gir ServerResponseException" to mockAltinn3M2MClient(content = "", HttpStatusCode.GatewayTimeout),
                    "GatewayTimeout fra Altinn OBO gir ServerResponseException" to mockAltinn3OBOClient(content = "", HttpStatusCode.GatewayTimeout),
                ),
            ) { altinn3Client ->
                when (altinn3Client) {
                    is Altinn3M2MClient ->
                        shouldThrowExactly<ServerResponseException> {
                            altinn3Client.hentTilganger(FNR)
                        }

                    is Altinn3OBOClient ->
                        shouldThrowExactly<ServerResponseException> {
                            altinn3Client.hentTilganger(FNR, { "" })
                        }

                    else -> throw IllegalStateException("Altinn3Client må være av typen Altinn3M2MClient eller Altinn3OBOClient")
                }
            }
        }
    })
