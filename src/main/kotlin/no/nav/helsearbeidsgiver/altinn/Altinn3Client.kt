package no.nav.helsearbeidsgiver.altinn

import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import jdk.jfr.Description
import kotlinx.serialization.Serializable
import no.nav.helsearbeidsgiver.utils.cache.LocalCache
import no.nav.helsearbeidsgiver.utils.cache.getIfCacheNotNull

/**
 * Klient som benytter Team Fager sitt API for å hente hvilke tilganger en innlogget bruker har i hvilke virksomheter/bedrifter.
 *
 * API-dokumentasjon her: https://arbeidsgiver-altinn-tilganger.intern.dev.nav.no/swagger-ui/index.html
 */
class Altinn3Client(
    private val baseUrl: String,
    private val serviceCode: String,
    private val m2m: Boolean = true,
    cacheConfig: CacheConfig? = null,
) {
    private val urlString = if (m2m) "$baseUrl/m2m/altinn-tilganger" else "$baseUrl/altinn-tilganger"
    private val httpClient = createHttpClient(maxRetries = 3)
    private val cache =
        cacheConfig?.let {
            LocalCache<TilgangResponse>(it.entryDuration, it.maxEntries)
        }
    val FILTER = Filter(altinn2Tilganger = setOf("$serviceCode:1"), altinn3Tilganger = emptySet())

    suspend fun hentHierarkiMedTilganger(
        fnr: String,
        getToken: () -> String,
    ): TilgangResponse =
        cache.getIfCacheNotNull(fnr) {
            val request = if (m2m) TilgangRequest(fnr, FILTER) else TilgangRequest(null, FILTER)
            httpClient
                .post(urlString) {
                    contentType(ContentType.Application.Json)
                    bearerAuth(getToken())
                    setBody(request)
                }.body<TilgangResponse>()
        }

    suspend fun hentTilganger(
        fnr: String,
        getToken: () -> String,
    ): Set<String> = hentHierarkiMedTilganger(fnr, getToken).tilgangTilOrgNr["$serviceCode:1"].orEmpty()

    suspend fun harTilgangTilOrganisasjon(
        fnr: String,
        orgnr: String,
        getToken: () -> String,
    ): Boolean = orgnr in hentTilganger(fnr, getToken)
}

@Serializable
data class TilgangResponse(
    val isError: Boolean,
    val hierarki: List<AltinnTilgang>,
    val tilgangTilOrgNr: Map<String, Set<String>>,
)

@Description("Brukerens tilganger til Altinn 2 og Altinn 3 for en organisasjon")
@Serializable
data class AltinnTilgang(
    @Description("Organisasjonsnummer") val orgnr: String,
    @Description("Tilganger til Altinn 3") val altinn3Tilganger: Set<String>,
    @Description("Tilganger til Altinn 2") val altinn2Tilganger: Set<String>,
    @Description("list av underenheter til denne organisasjonen hvor brukeren har tilganger") val underenheter: List<AltinnTilgang>,
    @Description("Navn på organisasjonen") val navn: String,
    @Description("Organisasjonsform. se https://www.brreg.no/bedrift/organisasjonsformer/") val organisasjonsform: String,
)

@Serializable
data class TilgangRequest(
    val fnr: String?,
    val filter: Filter,
)

@Serializable
data class Filter(
    val altinn2Tilganger: Set<String>,
    val altinn3Tilganger: Set<String>,
)
