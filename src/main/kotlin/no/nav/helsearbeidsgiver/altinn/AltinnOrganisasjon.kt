package no.nav.helsearbeidsgiver.altinn

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
    val navn: String,
    @JsonNames("Type")
    val type: String,
    @JsonNames("OrganizationNumber")
    val orgnr: String? = null,
    @JsonNames("OrganizationForm")
    val orgForm: String? = null,
    @JsonNames("Status")
    val status: String? = null,
    @JsonNames("ParentOrganizationNumber")
    val orgnrHovedenhet: String? = null
) {
    internal fun nullEmptyStrings(): AltinnOrganisasjon =
        copy(
            orgnr = orgnr?.orNull(),
            orgForm = orgForm?.orNull(),
            status = status?.orNull(),
            orgnrHovedenhet = orgnrHovedenhet?.orNull()
        )
}

private fun String.orNull(): String? =
    this.ifEmpty { null }
