package no.nav.helsearbeidsgiver.altinn

import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import no.nav.helsearbeidsgiver.utils.cache.LocalCache
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger

/**
 * Klient som benytter Team Fager sitt API for å hente hvilke tilganger en innlogget bruker har i hvilke virksomheter/bedrifter.
 *
 * API-dokumentasjon her: https://arbeidsgiver-altinn-tilganger.intern.dev.nav.no/swagger-ui/index.html
 */
class Altinn3OBOClient(
    baseUrl: String,
    private val serviceCode: String,
    val ressurs: Altinn3Ressurs,
    cacheConfig: LocalCache.Config,
) {
    private val sikkerLogger = sikkerLogger()

    private val urlString = "$baseUrl/altinn-tilganger"
    private val httpClient = createHttpClient()
    private val cache = LocalCache<AltinnTilgangRespons>(cacheConfig)

    private val tilgangRequest =
        TilgangOBORequest(
            filter =
                Filter(
                    altinn2Tilganger = setOf("$serviceCode:1"),
                    altinn3Tilganger = setOf(ressurs.value),
                ),
        )

    suspend fun hentHierarkiMedTilganger(
        fnr: String,
        getToken: () -> String,
    ): AltinnTilgangRespons =
        cache.getOrPut(fnr) {
            sikkerLogger.info("Henter Altinntilganger fra Fager sitt obo-endepunkt for ${fnr.take(6)}XXXX")

            httpClient
                .post(urlString) {
                    contentType(ContentType.Application.Json)
                    bearerAuth(getToken())
                    setBody(tilgangRequest)
                }.body<AltinnTilgangRespons>()
                .also { respons ->
                    sikkerLogger.info("Hentet Altinntilganger for ${fnr.take(6)}XXXX med ${respons.hierarki.size} hovedenheter.")
                }
        }

    suspend fun hentTilganger(
        fnr: String,
        getToken: () -> String,
    ): Set<String> =
        hentHierarkiMedTilganger(fnr, getToken).let {
            val altinn2Tilganger =
                it.organisasjonerMedAltinn2Tilgang()
            val altinn3Tilganger = it.organisasjonerMedAltinn3Tilgang()
            val diffTilganger = altinn2Tilganger.minus(altinn3Tilganger)
            sikkerLogger().info(
                "Hentet altinn tilganger for ${fnr.take(6)}XXXXX: diff antall: ${diffTilganger.size}, diff: $diffTilganger",
            )
            altinn2Tilganger + altinn3Tilganger
        }

    fun AltinnTilgangRespons.organisasjonerMedAltinn2Tilgang(): Set<String> = tilgangTilOrgNr["$serviceCode:1"].orEmpty()

    fun AltinnTilgangRespons.organisasjonerMedAltinn3Tilgang(): Set<String> = tilgangTilOrgNr[ressurs.value].orEmpty()

    suspend fun harTilgangTilOrganisasjon(
        fnr: String,
        orgnr: String,
        getToken: () -> String,
    ): Boolean = orgnr in hentTilganger(fnr, getToken)
}

@Serializable
data class TilgangOBORequest(
    val filter: Filter,
)
