package no.nav.toi.stilling.publiser.arbeidsplassen

import no.nav.toi.stilling.publiser.arbeidsplassen.dto.ArbeidsplassenStilling
import no.nav.toi.stilling.publiser.arbeidsplassen.dto.ContactArbeidsplassen
import no.nav.toi.stilling.publiser.arbeidsplassen.dto.Employer
import no.nav.toi.stilling.publiser.arbeidsplassen.dto.Location
import no.nav.toi.stilling.publiser.arbeidsplassen.dto.PrivacyType
import no.nav.toi.stilling.publiser.arbeidsplassen.dto.PropertyName
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Sett IMPORT_API_AUTHORIZATION_TOKEN som env variabel for å kjøre denne testen.
 */
@Disabled(value = "Denne testen skal kun kjøres manuelt ved behov")
class ArbeidsplassenRestKlientImplIT {

    val baseUrl = URI("https://arbeidsplassen-api.ekstern.dev.nav.no")
    val token = System.getenv().get("IMPORT_API_AUTHORIZATION_TOKEN") ?: error("IMPORT_API_AUTHORIZATION_TOKEN mangler - sett som env variabel")

    @Test
    fun `Opprett eller oppdater stilling`() {
        val stilling = ArbeidsplassenStilling(
            reference = "123",
            positions = 1,
            title = "Test fra TOI",
            adText = "Dette er et fint sted å jobbe!",
            published = LocalDateTime.now().toIsoDateTimeString(),
            expires =  LocalDateTime.now().plusDays(30).toIsoDateTimeString(),
            locationList = listOf(
                Location(
                    address = "Adresse",
                    postalCode = "4020",
                    county = "Rogaland",
                    city = "Stavanger",
                    country = "Norge",
                )
            ),
            contactList = listOf(
                ContactArbeidsplassen(
                    name = "Navn",
                    email = "test@nav.no",
                    phone = "11111111",
                )
            ),
            employer = Employer(
                businessName = "Kulsveen Logistikk",
                orgnr = "215271242",
                location = Location(
                    address = "Fyrstikkalléen 1",
                    postalCode = "0661",
                    city = "Oslo",
                    country = "Norge",
                )
            ),
            privacy = PrivacyType.SHOW_ALL,
            properties = mapOf(
                PropertyName.applicationdue to LocalDate.now().plusDays(30).format(DateTimeFormatter.ISO_DATE),
                PropertyName.applicationemail to "test@nav.no",
                PropertyName.engagementtype to "Fast",
                PropertyName.workday to "[\"Ukedager\"]",
                PropertyName.workhours to "[\"Dagtid\"]",
                PropertyName.jobarrangement to "Skift",
                PropertyName.sector to "Privat",
            ),
        )
        ArbeidsplassenRestKlientImpl(baseUrl, token).publiserStilling(stilling)
    }
}
