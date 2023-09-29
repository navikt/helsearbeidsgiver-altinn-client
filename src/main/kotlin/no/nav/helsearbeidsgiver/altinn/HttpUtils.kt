package no.nav.helsearbeidsgiver.altinn

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.apache5.Apache5
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import no.nav.helsearbeidsgiver.utils.json.jsonConfig

internal fun createHttpClient(): HttpClient =
    HttpClient(Apache5) { customize() }

internal fun HttpClientConfig<*>.customize() {
    expectSuccess = true

    install(ContentNegotiation) {
        json(jsonConfig)
    }
}
