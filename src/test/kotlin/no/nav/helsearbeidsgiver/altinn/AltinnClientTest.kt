package no.nav.helsearbeidsgiver.altinn

import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeExactly
import io.ktor.client.plugins.ServerResponseException
import io.ktor.http.HttpStatusCode

class AltinnClientTest : StringSpec({

    "valid answer from altinn returns properly serialized list of all active org forms" {
        val authList = buildClient(HttpStatusCode.OK, validAltinnResponse).hentOrgMedRettigheterForPerson(identitetsnummer)

        authList.size shouldBeExactly 4
    }

    "timeout from altinn throws exception" {
        shouldThrowExactly<ServerResponseException> {
            buildClient(HttpStatusCode.GatewayTimeout, "").hentOrgMedRettigheterForPerson(identitetsnummer)
        }
    }

    "timeout from altinn throws AltinnBrukteForLangTidException" {
        shouldThrowExactly<AltinnBrukteForLangTidException> {
            buildClient(HttpStatusCode.BadGateway, "").hentOrgMedRettigheterForPerson(identitetsnummer)
        }
    }
})
