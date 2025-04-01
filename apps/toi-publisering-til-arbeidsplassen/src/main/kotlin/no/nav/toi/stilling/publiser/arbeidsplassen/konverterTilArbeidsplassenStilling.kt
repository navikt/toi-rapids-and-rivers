package no.nav.toi.stilling.publiser.arbeidsplassen

import no.nav.toi.stilling.publiser.arbeidsplassen.dto.ArbeidsplassenStilling
import no.nav.toi.stilling.publiser.arbeidsplassen.dto.ContactArbeidsplassen
import no.nav.toi.stilling.publiser.arbeidsplassen.dto.DirektemeldtStilling
import no.nav.toi.stilling.publiser.arbeidsplassen.dto.Employer
import no.nav.toi.stilling.publiser.arbeidsplassen.dto.Location
import no.nav.toi.stilling.publiser.arbeidsplassen.dto.PrivacyType
import no.nav.toi.stilling.publiser.arbeidsplassen.dto.PropertyName
import java.time.LocalDateTime


fun konverterTilArbeidsplassenStilling(direktemeldtStilling: DirektemeldtStilling): ArbeidsplassenStilling {
    val innhold = direktemeldtStilling.innhold
    val properties = innhold.properties
    val employer = innhold.employer

    val arbeidsplassenStilling = ArbeidsplassenStilling(
        reference = direktemeldtStilling.stillingsid.toString(),
        positions = properties["positioncount"]?.toInt() ?: 1,
        title = innhold.title,
        adText = properties["adtext"] ?: "",
        published = innhold.published?.toIsoDateTimeString() ?: LocalDateTime.now().toIsoDateTimeString(),
        expires = innhold.expires?.toIsoDateTimeString(),
        privacy = PrivacyType.fromString(innhold.privacy ?: ""),
        contactList = innhold.contactList.filter { it.name != null }.map {
            ContactArbeidsplassen(
                name = it.name!!,
                email = it.email,
                phone = it.phone,
                title = it.title,
            )
        },
        employer = Employer(
            businessName = employer.name ?: "",
            orgnr = employer.orgnr ?: "",
            location = Location(
                address = employer.location?.address,
                postalCode = employer.location?.postalCode,
                city = employer.location?.city,
                municipal = employer.location?.municipal,
                county = employer.location?.county,
                country = employer.location?.country,
            )
        ),
        locationList = innhold.locationList.map {
            Location(
                address = it.address,
                postalCode = it.postalCode,
                city = it.city,
                municipal = it.municipal,
                county = it.county,
                country = it.country,
            )
        },
        properties = mapOf(
            PropertyName.applicationemail to (properties["applicationemail"] ?: ""),
            PropertyName.applicationdue to (properties["applicationdue"] ?: ""),
            PropertyName.engagementtype to (properties["engagementtype"] ?: ""),
            PropertyName.extent to (properties["extent"] ?: ""),
            PropertyName.jobarrangement to (properties["jobarrangement"] ?: ""),
            PropertyName.sector to (properties["sector"] ?: ""),
            PropertyName.starttime to (properties["starttime"] ?: ""),
            PropertyName.workday to (properties["workday"] ?: ""),
            PropertyName.workhours to (properties["workhours"] ?: ""),
        )
    )
    return arbeidsplassenStilling
}
