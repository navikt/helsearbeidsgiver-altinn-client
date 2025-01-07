package no.nav.helsearbeidsgiver.altinn

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.apache5.Apache5
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import no.nav.helsearbeidsgiver.utils.json.jsonConfig

internal fun createHttpClient(
    maxRetries: Int,
    getToken: () -> String,
): HttpClient = HttpClient(Apache5) { configure(maxRetries, getToken) }

internal fun HttpClientConfig<*>.configure(
    retries: Int,
    getToken: () -> String,
) {
    expectSuccess = true
    install(ContentNegotiation) {
        json(jsonConfig)
    }
    var token: BearerTokens
    install(Auth) {
        bearer {
            loadTokens {
                token = BearerTokens(getToken(), "")
                token
            }
            refreshTokens {
                token = BearerTokens(getToken(), "")
                token
            }
        }
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
        is java.net.SocketTimeoutException -> true
        else -> false
    }
