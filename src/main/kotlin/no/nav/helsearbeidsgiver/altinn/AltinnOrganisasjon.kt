package no.nav.helsearbeidsgiver.altinn

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

/**
 * En organisasjon slik de blir returnert fra Altinn.

 * @property name The name of the reportee.
 * @property type The type of reportee. Value depends on the language choice of the user.
 * ```
 * English: Enterprise | Business | Person
 * Bokmål: Foretak | Bedrift | Person
 * Nynorsk: Føretak | Bedrift | Person
 * ```
 * @property orgNo The organization number of the reportee. This is populated only if the reportee is an organization.
 * @property orgNoParent The parent organization number of the reportee. This is populated only if the reportee is an organization, and the organization is a suborganization.
 * @property orgForm The type of organization for the reportee. This is populated only if the reportee is an organization. E.g ENK, AS, ORGL.
 * @property status	Indicates whether the organization is active or inactive. An organization can be inactive if it for some reason (e.g. bankruptcy) has ceased. This is populated only if the reportee is an organization.
 * @property nin The national identity number (or social security number) of the reportee. This is populated only if the reportee is a person.
 */
@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class AltinnOrganisasjon(
    @JsonNames("Name")
    val name: String,
    @JsonNames("Type")
    val type: String,
    @JsonNames("OrganizationNumber")
    val orgNo: String? = null,
    @JsonNames("ParentOrganizationNumber")
    val orgNoParent: String? = null,
    @JsonNames("OrganizationForm")
    val orgForm: String? = null,
    @JsonNames("Status")
    val status: String? = null,
    @JsonNames("SocialSecurityNumber")
    val nin: String? = null
) {
    internal fun nullEmptyStrings(): AltinnOrganisasjon =
        copy(
            orgNo = orgNo?.orNull(),
            orgNoParent = orgNoParent?.orNull(),
            orgForm = orgForm?.orNull(),
            status = status?.orNull(),
            nin = nin?.orNull()
        )
}

private fun String.orNull(): String? =
    this.ifEmpty { null }
