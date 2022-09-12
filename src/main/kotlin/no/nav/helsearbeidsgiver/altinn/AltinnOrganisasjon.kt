package no.nav.helsearbeidsgiver.altinn

import kotlinx.serialization.Serializable

/**
 * En organisasjon slik de blir returnert fra Altinn.

 * @property name The name of the reportee.
 * @property type The type of reportee. Value depends on the language choice of the user.
 * ```
 * English: Enterprise | Business | Person
 * Bokmål: Foretak | Bedrift | Person
 * Nynorsk: Føretak | Bedrift | Person
 * ```
 * @property organizationNumber The organization number of the reportee. This is populated only if the reportee is an organization.
 * @property organizationForm The type of organization for the reportee. This is populated only if the reportee is an organization. E.g ENK, AS, ORGL.
 * @property parentOrganizationNumber The parent organization number of the reportee. This is populated only if the reportee is an organization, and the organization is a suborganization.
 * @property socialSecurityNumber The social security number of the reportee. This is populated only if the reportee is a person.
 * @property status	Indicates whether the organization is active or inactive. An organization can be inactive if it for some reason (e.g. bankruptcy) has ceased. This is populated only if the reportee is an organization.
 */
@Serializable
data class AltinnOrganisasjon(
    val name: String,
    val type: String,
    val organizationNumber: String? = null,
    val organizationForm: String? = null,
    val parentOrganizationNumber: String? = null,
    val socialSecurityNumber: String? = null,
    val status: String? = null
)
