package no.nav.helsearbeidsgiver.altinn

import io.ktor.client.call.body
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpStatusCode
import no.nav.helsearbeidsgiver.utils.cache.LocalCache
import no.nav.helsearbeidsgiver.utils.cache.getIfCacheNotNull
import no.nav.helsearbeidsgiver.utils.log.logger
import kotlin.time.Duration

private const val PAGE_SIZE = 500

/**
 * Klient som benytter Altinns REST API for å hente organisasjoner som en gitt bruker har tilgang til for den gitte tjenesten.
 *
 * Dokumentasjon her: https://www.altinn.no/api/serviceowner/help
 *
 * For å få tilgang til dette APIet må det settes opp tilgang gjennom API-Gateway til tjenesten (vi går ikke mot den offesielle Altinn URLen).
 * For hjelp med dette spør i #apigw
 */
class AltinnClient(
    private val url: String,
    private val serviceCode: String,
    private val getToken: () -> String,
    private val altinnApiKey: String,
    cacheConfig: CacheConfig? = null,
) {
    private val logger = this.logger()

    private val httpClient = createHttpClient()

    private val cache = cacheConfig?.let {
        LocalCache<Set<AltinnOrganisasjon>>(it.entryDuration, it.maxEntries)
    }

    init {
        logger.debug(
            """AltinnClient-config:
                    url: $url
                    serviceCode: $serviceCode
                    altinnApiKey: ${altinnApiKey.take(1)}.....
            """.trimIndent(),
        )
    }

    /**
     * Sjekker om [identitetsnummer] har rettighet til å se refusjoner for [organisasjonId], og om [organisasjonId] er en underenhet (virksomhet).
     *
     * @param organisasjonId Kan være virksomhet, hovedenhet, privatperson eller organisasjonsledd.
     */
    suspend fun harRettighetForOrganisasjon(identitetsnummer: String, organisasjonId: String): Boolean =
        hentRettighetOrganisasjoner(identitetsnummer)
            .any {
                it.orgnr == organisasjonId &&
                    it.orgnrHovedenhet != null
            }

    /**
     * @return Liste over organisasjoner og/eller personer hvor den angitte personen har tilknyttede rettigheter.
     */
    suspend fun hentRettighetOrganisasjoner(identitetsnummer: String): Set<AltinnOrganisasjon> =
        cache.getIfCacheNotNull(identitetsnummer) {
            logger.debug("Henter organisasjoner for ${identitetsnummer.take(5)}XXXXX")

            val callStart = System.currentTimeMillis()

            val rettighetOrganisasjoner = hashSetOf<AltinnOrganisasjon>()
            var pageNo = 0

            do {
                val rettighetOrganisasjonerForPage = hentRettighetOrganisasjonerForPage(identitetsnummer, pageNo)

                rettighetOrganisasjoner.addAll(rettighetOrganisasjonerForPage)

                pageNo++
            } while (rettighetOrganisasjonerForPage.size >= PAGE_SIZE)

            logger.debug("Altinn brukte ${System.currentTimeMillis() - callStart} ms på å svare med ${rettighetOrganisasjoner.size} rettigheter")

            rettighetOrganisasjoner
        }

    private suspend fun hentRettighetOrganisasjonerForPage(id: String, pageNo: Int): Set<AltinnOrganisasjon> {
        val url = buildUrl(id, pageNo)
        return try {
            httpClient.get(url) {
                bearerAuth(getToken())
                header("APIKEY", altinnApiKey)
            }
                .body<Set<AltinnOrganisasjon>>()
                .map(AltinnOrganisasjon::nullEmptyStrings)
                .toSet()
        } catch (e: ServerResponseException) {
            if (e.response.status == HttpStatusCode.BadGateway) {
                // Midlertidig hook for å detektere at det tok for lang tid å hente rettigheter
                // Brukeren/klienten kan prøve igjen når dette skjer siden altinn svarer raskere gang nummer 2
                logger.warn("Fikk en timeout fra Altinn som vi antar er fiksbar lagg hos dem", e)
                throw AltinnBrukteForLangTidException()
            } else {
                throw e
            }
        }
    }

    private fun buildUrl(id: String, pageNo: Int) = "$url/reportees/" +
        "?ForceEIAuthentication" +
        "&\$filter=Type+ne+'Person'+and+Status+eq+'Active'" +
        "&serviceCode=$serviceCode" +
        "&serviceEdition=1" +
        "&subject=$id" +
        "&\$top=$PAGE_SIZE" +
        "&\$skip=${pageNo * PAGE_SIZE}"
}

data class CacheConfig(
    val entryDuration: Duration,
    val maxEntries: Int,
)

class AltinnBrukteForLangTidException : Exception(
    "Altinn brukte for lang tid til å svare på forespørselen om tilganger. Prøv igjen om litt.",
)
