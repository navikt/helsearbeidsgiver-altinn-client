package no.nav.helsearbeidsgiver.altinn

import jdk.jfr.Description
import kotlinx.serialization.Serializable

@Description("Brukerens tilganger til Altinn 2 og Altinn 3 for en organisasjon")
@Serializable
data class AltinnTilgang(
    @Description("Organisasjonsnummer") val orgnr: String,
    @Description("Tilganger til Altinn 3") val altinn3Tilganger: Set<String>,
    @Description("Tilganger til Altinn 2") val altinn2Tilganger: Set<String>,
    @Description("list av underenheter til denne organisasjonen hvor brukeren har tilganger") val underenheter: List<AltinnTilgang>,
    @Description("Navn på organisasjonen") val navn: String,
    @Description("Organisasjonsform. se https://www.brreg.no/bedrift/organisasjonsformer/") val organisasjonsform: String,
)
