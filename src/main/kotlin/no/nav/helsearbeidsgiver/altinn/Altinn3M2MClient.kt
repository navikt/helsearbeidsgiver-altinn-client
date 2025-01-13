package no.nav.helsearbeidsgiver.altinn

import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import no.nav.helsearbeidsgiver.utils.cache.LocalCache
import no.nav.helsearbeidsgiver.utils.cache.getIfCacheNotNull

/**
 * Klient som benytter Team Fager sitt API for å hente hvilke tilganger en innlogget bruker har i hvilke virksomheter/bedrifter.
 *
 * API-dokumentasjon her: https://arbeidsgiver-altinn-tilganger.intern.dev.nav.no/swagger-ui/index.html
 */

class Altinn3M2MClient(
    private val baseUrl: String,
    private val serviceCode: String,
    private val getToken: () -> String,
    cacheConfig: CacheConfig? = null,
) {
    private val urlString = "$baseUrl/m2m/altinn-tilganger"
    private val httpClient = createHttpClient(maxRetries = 3)
    private val cache =
        cacheConfig?.let {
            LocalCache<AltinnTilgangRespons>(it.entryDuration, it.maxEntries)
        }
    private val tilgangFilter = Filter(altinn2Tilganger = setOf("$serviceCode:1"), altinn3Tilganger = emptySet())

    suspend fun hentHierarkiMedTilganger(fnr: String): AltinnTilgangRespons =
        cache.getIfCacheNotNull(fnr) {
            val request = TilgangM2MRequest(fnr, tilgangFilter)
            httpClient
                .post(urlString) {
                    contentType(ContentType.Application.Json)
                    bearerAuth(getToken())
                    setBody(request)
                }.body<AltinnTilgangRespons>()
        }

    suspend fun hentTilganger(fnr: String): Set<String> = hentHierarkiMedTilganger(fnr).tilgangTilOrgNr["$serviceCode:1"].orEmpty()

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
