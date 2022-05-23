package no.nav.helsearbeidsgiver.altinn

import io.ktor.client.features.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class AltinnClientTest {

    @Test
    internal fun `valid answer from altinn returns properly serialized list of all active org forms`() {
        val authList = buildClient(HttpStatusCode.OK, validAltinnResponse).hentOrgMedRettigheterForPerson(identitetsnummer)
        assertEquals(4, authList.size)
    }

    @Test
    internal fun `timeout from altinn throws exception`() {
        assertThrows(ServerResponseException::class.java) {
            buildClient(HttpStatusCode.GatewayTimeout, "").hentOrgMedRettigheterForPerson(identitetsnummer)
        }
    }

    @Test
    internal fun `timeout from altinn throws AltinnBrukteForLangTidException`() {
        assertThrows(AltinnBrukteForLangTidException::class.java) {
            runBlocking { buildClient(HttpStatusCode.BadGateway, "").hentOrgMedRettigheterForPerson(identitetsnummer) }
        }
    }
}
