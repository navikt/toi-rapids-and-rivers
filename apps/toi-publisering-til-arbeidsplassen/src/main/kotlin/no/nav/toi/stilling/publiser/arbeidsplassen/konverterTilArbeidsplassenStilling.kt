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
        reference = direktemeldtStilling.stillingsId.toString(),
        positions = properties["positioncount"]?.toInt() ?: 1,
        title = innhold.title,
        adText = properties["adtext"] ?: "",
        published = LocalDateTime.now().toIsoDateTimeString(), //  Må være et felt som endres ved publisering eller avpublisering
        expires = fjernTidssoneOmAngitt(direktemeldtStilling.utløpsdato),
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
            PropertyName.applicationurl to (properties["applicationurl"] ?: ""),
            PropertyName.applicationdue to (properties["applicationdue"] ?: ""),
            PropertyName.employerdescription to (properties["employerdescription"] ?: ""),
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

/** Datoene vi får inn er på norsk tid når man tar bort tidssonen. Import api'et støtter ikke tidssone */
fun fjernTidssoneOmAngitt(dato: String?): String? {
    if (dato == null) return null
    return dato
        .replace("+00:00", "")
        .replace("+01:00", "")
        .replace("+02:00", "")
}
