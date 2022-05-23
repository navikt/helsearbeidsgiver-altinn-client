package no.nav.helsearbeidsgiver.altinn

import io.ktor.http.*

internal fun buildClient(status: HttpStatusCode, content: String): AltinnRestClient {
    return AltinnRestClient(
        "url",
        "",
        "",
        "",
        mockHttpClient(status, content),
        5
    )
}

val validAltinnResponse = "altinn-mock-data/organisasjoner-med-rettighet.json".loadFromResources()

val identitetsnummer = "01020354321"
