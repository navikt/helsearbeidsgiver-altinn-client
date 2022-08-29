package no.nav.helsearbeidsgiver.altinn

import io.ktor.http.HttpStatusCode

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

val validAltinnResponse = "organisasjoner-med-rettighet.json".loadFromResources()

val identitetsnummer = "01020354321"
