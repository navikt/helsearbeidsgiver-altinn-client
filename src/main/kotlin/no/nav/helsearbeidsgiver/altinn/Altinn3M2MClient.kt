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
 * API-dokumentasjon her: https://arbeidsgiver-altinn-tilganger.intern.dev.nav.no/swagger-ui
 */

class Altinn3M2MClient(
    baseUrl: String,
    val ressurs: Altinn3Ressurs,
    cacheConfig: LocalCache.Config,
    private val getToken: () -> String,
) {
    private val sikkerLogger = sikkerLogger()

    private val urlString = "$baseUrl/m2m/altinn-tilganger"
    private val httpClient = createHttpClient()
    private val cache = LocalCache<Set<String>>(cacheConfig)

    private val tilgangFilter = Filter(altinn2Tilganger = emptySet(), altinn3Tilganger = setOf(ressurs.value))

    internal suspend fun hentHierarkiMedTilganger(fnr: String): AltinnTilgangRespons {
        sikkerLogger.info("Henter Altinntilganger fra Fager sitt m2m-endepunkt for ${fnr.take(6)}XXXX")
        val request = TilgangM2MRequest(fnr, tilgangFilter)
        httpClient
            .post(urlString) {
                contentType(ContentType.Application.Json)
                bearerAuth(getToken())
                setBody(request)
            }.body<AltinnTilgangRespons>()
            .also { respons ->
                sikkerLogger.info("Hentet Altinntilganger for ${fnr.take(6)}XXXX med ${respons.hierarki.size} hovedenheter.")

                return respons
            }
    }

    suspend fun hentTilganger(fnr: String): Set<String> =
        cache.getOrPut(fnr) {
            hentHierarkiMedTilganger(fnr).let {
                val altinn3Tilganger = it.organisasjonerMedAltinn3Tilgang()
                sikkerLogger().info(
                    "Hentet altinn tilganger for ${fnr.take(6)}XXXXX: antall altinn3-tilganger: ${altinn3Tilganger.size}",
                )
                altinn3Tilganger
            }
        }

    fun AltinnTilgangRespons.organisasjonerMedAltinn3Tilgang(): Set<String> = tilgangTilOrgNr[ressurs.value].orEmpty()

    suspend fun harTilgangTilOrganisasjon(
        fnr: String,
        orgnr: String,
    ): Boolean = orgnr in hentTilganger(fnr)
}

@Serializable
data class TilgangM2MRequest(
    val fnr: String,
    val filter: Filter,
)
