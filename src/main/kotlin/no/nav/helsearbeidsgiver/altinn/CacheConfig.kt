package no.nav.helsearbeidsgiver.altinn

import kotlin.time.Duration

data class CacheConfig(
    val entryDuration: Duration,
    val maxEntries: Int,
)
