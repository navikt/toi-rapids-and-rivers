package no.nav.toi.stilling.publiser.arbeidsplassen.dto

data class ArbeidsplassenStilling(
    val reference: String,
    val positions: Int,
    val title: String,
    val adText: String,
    val published: String,
    val expires: String?,
    val locationList: List<Location> = emptyList(),
    val contactList: List<ContactArbeidsplassen> = emptyList(),
    val employer: Employer,
    val privacy: PrivacyType = PrivacyType.SHOW_ALL,
    val properties: Map<PropertyName, String> = emptyMap(),
)

data class Location(
    val address: String? = null,
    val postalCode: String? = null,
    val city: String? = null,
    val municipal: String? = null,
    val county: String? = null,
    val country: String? = null,
)

data class ContactArbeidsplassen(
    val name: String,
    val email: String? = null,
    val phone: String? = null,
    val title: String? = null,
)

data class Employer(
    val businessName: String,
    val reference: String? = null,
    val orgnr: String? = null,
    val location: Location? = null,
)

enum class PropertyName {
    applicationdue,
    applicationemail,
    applicationurl,
    author,
    employerdescription,
    engagementtype,
    extent,
    jobarrangement,
    remote,
    sector,
    starttime,
    workday,
    workhours,
}

enum class PrivacyType {
    SHOW_ALL,
    INTERNAL_NOT_SHOWN;

    companion object {
        fun fromString(value: String): PrivacyType {
            return when (value) {
                "INTERNAL_NOT_SHOWN" -> INTERNAL_NOT_SHOWN
                else -> SHOW_ALL
            }
        }
    }
}
