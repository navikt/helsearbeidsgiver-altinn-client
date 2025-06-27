package no.nav.helsearbeidsgiver.altinn

import io.kotest.assertions.throwables.shouldNotThrowAny
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
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import no.nav.helsearbeidsgiver.utils.test.resource.readResource
import kotlin.Pair
import kotlin.String

private val validAltinnResponse = "rettighetene-til-tanja-minge.json".readResource()

private const val FNR = "01020354321"

class Altinn3ClientTest :
    FunSpec({
        context("hent tilganger vellykket gir liste av organisasjoner") {
            withData(
                mapOf<String, suspend (Pair<HttpStatusCode, String>) -> Set<String>>(
                    "Altinn M2M" to { mockAltinn3M2MClient(it).hentTilganger(FNR) },
                    "Altinn OBO" to { mockAltinn3OBOClient(it).hentTilganger(FNR) { "" } },
                ),
            ) { hentTilganger ->
                val tilganger = hentTilganger(HttpStatusCode.OK to validAltinnResponse)

                tilganger.size shouldBeExactly 3
            }
        }

        context("fnr har kun rettigheter tilknyttet organisasjoner som Altinn returnerer og som er underenheter") {
            withData(
                mapOf<String, suspend (Pair<HttpStatusCode, String>, String) -> Boolean>(
                    "Altinn M2M" to { responses, orgnr -> mockAltinn3M2MClient(responses).harTilgangTilOrganisasjon(FNR, orgnr) },
                    "Altinn OBO" to { responses, orgnr -> mockAltinn3OBOClient(responses).harTilgangTilOrganisasjon(FNR, orgnr) { "" } },
                ),
            ) { harTilgangTilOrganisasjon ->
                listOf(
                    row("810007842", true),
                    row("810007702", false), // Er hovedenhet
                    row("123456789", false), // Er ikke i listen fra responsen
                ).forEach { (orgnr, expected) ->
                    val harTilgang =
                        harTilgangTilOrganisasjon(
                            HttpStatusCode.OK to validAltinnResponse,
                            orgnr,
                        )

                    withClue("$orgnr should yield $expected") {
                        harTilgang shouldBe expected
                    }
                }
            }
        }

        context("gyldig svar fra Altinn gir hierarki med liste av tilganger") {
            withData(
                mapOf<String, suspend (Pair<HttpStatusCode, String>) -> AltinnTilgangRespons>(
                    "Altinn M2M" to { mockAltinn3M2MClient(it).hentHierarkiMedTilganger(FNR) },
                    "Altinn OBO" to { mockAltinn3OBOClient(it).hentHierarkiMedTilganger(FNR) { "" } },
                ),
            ) { hentHierarkiMedTilganger ->
                val tilgangRespons = hentHierarkiMedTilganger(HttpStatusCode.OK to validAltinnResponse)

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

        listOf<Pair<String, suspend (Array<Pair<HttpStatusCode, String>>) -> Unit>>(
            "Altinn M2M" to { mockAltinn3M2MClient(*it).hentHierarkiMedTilganger(FNR) },
            "Altinn OBO" to { mockAltinn3OBOClient(*it).hentHierarkiMedTilganger(FNR) { "" } },
        )
            .forEach { (clientType, hentHierarkiMedTilganger) ->
                context(clientType) {
                    test("feiler ved 4xx-feil") {
                        val mockResponses = arrayOf(HttpStatusCode.NotFound to "")

                        val e = shouldThrowExactly<ClientRequestException> {
                            hentHierarkiMedTilganger(mockResponses)
                        }

                        e.response.status shouldBe HttpStatusCode.NotFound
                    }

                    test("lykkes ved færre 5xx-feil enn max retries (3)") {
                        val mockResponses =
                            arrayOf(
                                HttpStatusCode.InternalServerError to "",
                                HttpStatusCode.InternalServerError to "",
                                HttpStatusCode.InternalServerError to "",
                                HttpStatusCode.OK to validAltinnResponse,
                            )

                        runTest {
                            shouldNotThrowAny {
                                hentHierarkiMedTilganger(mockResponses)
                            }
                        }
                    }

                    test("feiler ved flere 5xx-feil enn max retries (3)") {
                        val mockResponses =
                            arrayOf(
                                HttpStatusCode.InternalServerError to "",
                                HttpStatusCode.InternalServerError to "",
                                HttpStatusCode.InternalServerError to "",
                                HttpStatusCode.InternalServerError to "",
                            )

                        runTest {
                            val e = shouldThrowExactly<ServerResponseException> {
                                hentHierarkiMedTilganger(mockResponses)
                            }

                            e.response.status shouldBe HttpStatusCode.InternalServerError
                        }
                    }

                    test("kall feiler og prøver på nytt ved timeout") {
                        val mockResponses =
                            arrayOf(
                                HttpStatusCode.OK to "timeout",
                                HttpStatusCode.OK to "timeout",
                                HttpStatusCode.OK to "timeout",
                                HttpStatusCode.OK to validAltinnResponse,
                            )

                        runTest {
                            shouldNotThrowAny {
                                hentHierarkiMedTilganger(mockResponses)
                            }
                        }
                    }
                }
            }
    })
