package no.nav.toi.stilling.publiser.arbeidsplassen

import no.nav.toi.stilling.publiser.arbeidsplassen.dto.Contact
import no.nav.toi.stilling.publiser.arbeidsplassen.dto.DirektemeldtStilling
import no.nav.toi.stilling.publiser.arbeidsplassen.dto.DirektemeldtStillingArbeidsgiver
import no.nav.toi.stilling.publiser.arbeidsplassen.dto.DirektemeldtStillingInnhold
import no.nav.toi.stilling.publiser.arbeidsplassen.dto.Geografi
import no.nav.toi.stilling.publiser.arbeidsplassen.dto.PrivacyType
import no.nav.toi.stilling.publiser.arbeidsplassen.dto.PropertyName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class KonverterTilArbeidsplassenStillingKtTest {

    @Test
    fun `Felter blir konvertert til riktig format`() {
         val direktemeldtStilling = DirektemeldtStilling(
             stillingsId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
             innhold  = DirektemeldtStillingInnhold(
                 title = "Tittel",
                 locationList = listOf(
                     Geografi(
                         address = "Adresse",
                         postalCode = "4020",
                         county = "Rogaland",
                         city = "Stavanger",
                         country = "Norge",
                     )
                 ),
                 contactList = listOf(
                     Contact(
                         name = "Navn",
                         email = "test@nav.no",
                         phone = "1234567890123456789012345678901234567890", // 40 tegn - skal bli trimmet til 36
                     )
                 ),
                 employer = DirektemeldtStillingArbeidsgiver(
                     name = "Bedrift",
                     orgnr = "123456789",
                     location = Geografi(
                         address = "Adresse",
                         postalCode = "1234",
                         country = "Norge",
                     ),
                 ),
                 privacy = "SHOW_ALL",
                 properties = mapOf(
                     "applicationdue" to LocalDate.of(2025, 4, 24).format(DateTimeFormatter.ISO_DATE),
                     "applicationemail" to "test@nav.no",
                     "applicationurl" to "https://www.nav.no/",
                     "engagementtype" to "Fast",
                     "sector" to "Privat",
                     "starttime" to "08.04.2025",
                     "workday" to "[\"Ukedager\"]",
                     "positioncount" to "1",
                     "adtext" to "<p>Jobb som utvikler</p>",
                     "employerdescription" to "<p>Om arbeidsgiver</p>",
                 ),
             ),
             annonsenr = "R123",
             opprettet = ZonedDateTime.of(LocalDateTime.of(2025, 1, 25, 14, 0), ZoneId.of("UTC")).toIsoDateTimeString(),
             opprettetAv = "pam-rekrutteringsbistand",
             sistEndret = ZonedDateTime.of(LocalDateTime.of(2025, 2, 25, 14, 0), ZoneId.of("UTC")).toIsoDateTimeString(),
             sistEndretAv = "pam-rekrutteringsbistand",
             status = "ACTIVE",
             publisert = ZonedDateTime.of(LocalDateTime.of(2025, 3, 25, 14, 0), ZoneId.of("UTC")).toIsoDateTimeString(),
             utløpsdato = ZonedDateTime.of(LocalDateTime.of(2025, 4, 25, 14, 0), ZoneId.of("UTC")).toIsoDateTimeString(),
        )

        val arbeidsplassenStilling = konverterTilArbeidsplassenStilling(direktemeldtStilling)

        assertEquals("123e4567-e89b-12d3-a456-426614174000", arbeidsplassenStilling.reference)
        assertEquals(1, arbeidsplassenStilling.positions)
        assertNotNull(arbeidsplassenStilling.published)
        assertEquals("2025-04-25T14:00:00", arbeidsplassenStilling.expires)
        assertEquals(PrivacyType.SHOW_ALL, arbeidsplassenStilling.privacy)
        assertEquals("Tittel", arbeidsplassenStilling.title)
        assertEquals("<p>Jobb som utvikler</p>", arbeidsplassenStilling.adText)

        assertEquals("Bedrift", arbeidsplassenStilling.employer.businessName)
        assertEquals("123456789", arbeidsplassenStilling.employer.orgnr)
        assertEquals("1234", arbeidsplassenStilling.employer.location?.postalCode)

        assertEquals(1, arbeidsplassenStilling.locationList.size)
        assertEquals("Adresse", arbeidsplassenStilling.locationList[0].address)
        assertEquals("4020", arbeidsplassenStilling.locationList[0].postalCode)

        assertEquals(1, arbeidsplassenStilling.contactList.size)
        assertEquals("Navn", arbeidsplassenStilling.contactList[0].name)
        assertEquals("123456789012345678901234567890123456", arbeidsplassenStilling.contactList[0].phone)

        assertEquals("Privat", arbeidsplassenStilling.properties[PropertyName.sector])
        assertEquals("[\"Ukedager\"]", arbeidsplassenStilling.properties[PropertyName.workday])
        assertEquals("Fast", arbeidsplassenStilling.properties[PropertyName.engagementtype])
        assertEquals("08.04.2025", arbeidsplassenStilling.properties[PropertyName.starttime])
        assertEquals("<p>Om arbeidsgiver</p>", arbeidsplassenStilling.properties[PropertyName.employerdescription])
        assertEquals("test@nav.no", arbeidsplassenStilling.properties[PropertyName.applicationemail])
        assertEquals("https://www.nav.no/", arbeidsplassenStilling.properties[PropertyName.applicationurl])
    }
}
