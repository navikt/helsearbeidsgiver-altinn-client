package no.nav.helsearbeidsgiver.altinn

import kotlinx.serialization.Serializable

@Serializable
data class AltinnTilgangRespons(
    val isError: Boolean,
    val hierarki: List<AltinnTilgang>,
    val tilgangTilOrgNr: Map<String, Set<String>>,
)
