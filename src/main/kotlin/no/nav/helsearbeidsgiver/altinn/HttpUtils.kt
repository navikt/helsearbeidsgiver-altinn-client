package no.nav.helsearbeidsgiver.altinn

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.apache5.Apache5
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import no.nav.helsearbeidsgiver.utils.json.jsonConfig

internal fun createHttpClient(maxRetries: Int): HttpClient = HttpClient(Apache5) { configure(maxRetries) }

internal fun HttpClientConfig<*>.configure(retries: Int) {
    expectSuccess = true
    install(ContentNegotiation) {
        json(jsonConfig)
    }
    install(HttpRequestRetry) {
        maxRetries = retries
        retryOnServerErrors(maxRetries)
        retryOnExceptionIf { _, cause ->
            cause.isRetryableException()
        }
        exponentialDelay()
    }
}

private fun Throwable.isRetryableException() =
    when (this) {
        is SocketTimeoutException -> true
        is ConnectTimeoutException -> true
        is HttpRequestTimeoutException -> true
        else -> false
    }
