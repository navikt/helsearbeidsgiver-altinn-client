package no.nav.helsearbeidsgiver.altinn

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDateTime

/**
 * Klient som benytter Altinns REST API for å hente organisasjoner som en gitt bruker har tilgang til for den gitte tjenesten.
 *
 * Dokumentasjon her: https://www.altinn.no/api/serviceowner/help
 *
 * For å få tilgang til dette APIet må det settes opp tilgang gjennom API-Gateway til tjenesten (vi går ikke mot den offesielle Altinn URLen).
 * For hjelp med dette spør i #apigw
 *
 */
class AltinnRestClient(
    altinnBaseUrl: String,
    private val apiGwApiKey: String,
    private val altinnApiKey: String,
    serviceCode: String,
    private val httpClient: HttpClient,
    private val pageSize: Int = 500
) : AltinnOrganisationsRepository {

    private val logger = LoggerFactory.getLogger("AltinnClient")

    init {
        logger.debug(
            """Altinn Config:
                    altinnBaseUrl: $altinnBaseUrl
                    apiGwApiKey: ${apiGwApiKey.take(1)}.....
                    altinnApiKey: ${altinnApiKey.take(1)}.....
                    serviceCode: $serviceCode
            """.trimIndent()
        )
    }

    private val baseUrl = "$altinnBaseUrl/reportees/?ForceEIAuthentication&\$filter=Type+ne+'Person'+and+Status+eq+'Active'&" +
        "serviceCode=$serviceCode&serviceEdition=1&&subject="

    /**
     * @return en liste over organisasjoner og/eller personer den angitte personen har rettigheten for
     */
    override fun hentOrgMedRettigheterForPerson(identitetsnummer: String): Set<AltinnOrganisasjon> {
        logger.debug("Henter organisasjoner for ${identitetsnummer.take(5)}XXXXX")

        val url = baseUrl + identitetsnummer
        return runBlocking {
            try {
                val allAccessRights = HashSet<AltinnOrganisasjon>()
                val start = LocalDateTime.now()
                var page = 0
                do {
                    val urlWithPagesizeAndOffset = url + "&\$top=" + pageSize + "&\$skip=" + page * pageSize
                    val pageResults = httpClient.get(urlWithPagesizeAndOffset) {
                        expectSuccess = true
                        headers.append("X-NAV-APIKEY", apiGwApiKey)
                        headers.append("APIKEY", altinnApiKey)
                    }
                        .body<Set<AltinnOrganisasjon>>()
                    allAccessRights.addAll(pageResults)
                    page++
                } while (pageResults.size >= pageSize)

                logger.debug("Altinn brukte ${Duration.between(start, LocalDateTime.now()).toMillis()}ms på å svare med ${allAccessRights.size} rettigheter")
                return@runBlocking allAccessRights
            } catch (e: Exception) {
                when {
                    e is ServerResponseException && e.response.status == HttpStatusCode.BadGateway -> {
                        // midlertidig hook for å detektere at det tok for lang tid å hente rettigheter
                        // brukeren/klienten kan prøve igjen når dette skjer siden altinn svarer raskere gang nummer 2
                        logger.warn("Fikk en timeout fra Altinn som vi antar er fiksbar lagg hos dem", e)
                        throw AltinnBrukteForLangTidException()
                    }
                    else -> throw e
                }
            }
        }
    }
}

class AltinnBrukteForLangTidException : Exception(
    "Altinn brukte for lang tid til å svare på forespørselen om tilganger. Prøv igjen om litt."
)
