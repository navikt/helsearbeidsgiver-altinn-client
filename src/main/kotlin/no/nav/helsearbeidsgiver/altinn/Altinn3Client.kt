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

class Altinn3Client(
    private val baseUrl: String,
    private val serviceCode: String,
    private val getToken: () -> String,
    cacheConfig: CacheConfig? = null,
) {
    private val httpClient = createHttpClient()

    private val cache =
        cacheConfig?.let {
            LocalCache<Set<String>>(it.entryDuration, it.maxEntries)
        }

    suspend fun harTilgangTilOrganisasjon(
        fnr: String,
        orgnr: String,
    ): Boolean = orgnr in hentTilganger(fnr)

    suspend fun hentTilganger(fnr: String): Set<String> =
        cache.getIfCacheNotNull(fnr) {
            val request = TilgangRequest(fnr)

            httpClient
                .post("$baseUrl/m2m/altinn-tilganger") {
                    bearerAuth(getToken())
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }.body<TilgangResponse>()
                .tilgangTilOrgNr["$serviceCode:1"]
                .orEmpty()
        }
}

@Serializable
data class TilgangResponse(
    val tilgangTilOrgNr: Map<String, Set<String>>,
)

@Serializable
data class TilgangRequest(
    val fnr: String,
)
