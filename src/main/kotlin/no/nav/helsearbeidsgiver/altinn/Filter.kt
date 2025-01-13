package no.nav.helsearbeidsgiver.altinn

import kotlinx.serialization.Serializable

@Serializable
data class Filter(
    val altinn2Tilganger: Set<String>,
    val altinn3Tilganger: Set<String>,
)
