package no.nav.helsearbeidsgiver.altinn

import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

/**
 * En organisasjon slik de blir returnert fra Altinn.

 * @property navn The name of the reportee.
 * @property type The type of reportee. Value depends on the language choice of the user.
 * ```
 * English: Enterprise | Business | Person
 * Bokmål: Foretak | Bedrift | Person
 * Nynorsk: Føretak | Bedrift | Person
 * ```
 * @property orgnr The organization number of the reportee. This is populated only if the reportee is an organization.
 * @property orgForm The type of organization for the reportee. This is populated only if the reportee is an organization. E.g ENK, AS, ORGL.
 * @property status	Indicates whether the organization is active or inactive. An organization can be inactive if it for some reason (e.g. bankruptcy) has ceased. This is populated only if the reportee is an organization.
 * @property orgnrHovedenhet The parent organization number of the reportee. This is populated only if the reportee is an organization, and the organization is a suborganization.
 */
@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class AltinnOrganisasjon(
    @JsonNames("Name")
    @JsonProperty("name")
    val navn: String,
    @JsonNames("Type")
    @JsonProperty("type")
    val type: String,
    @JsonNames("OrganizationNumber")
    @JsonProperty("organizationNumber")
    val orgnr: String? = null,
    @JsonNames("OrganizationForm")
    @JsonProperty("organizationForm")
    val orgForm: String? = null,
    @JsonNames("Status")
    @JsonProperty("status")
    val status: String? = null,
    @JsonNames("ParentOrganizationNumber")
    @JsonProperty("parentOrganizationNumber")
    val orgnrHovedenhet: String? = null,
) {
    internal fun nullEmptyStrings(): AltinnOrganisasjon =
        copy(
            orgnr = orgnr?.orNull(),
            orgForm = orgForm?.orNull(),
            status = status?.orNull(),
            orgnrHovedenhet = orgnrHovedenhet?.orNull(),
        )
}

private fun String.orNull(): String? =
    this.ifEmpty { null }
