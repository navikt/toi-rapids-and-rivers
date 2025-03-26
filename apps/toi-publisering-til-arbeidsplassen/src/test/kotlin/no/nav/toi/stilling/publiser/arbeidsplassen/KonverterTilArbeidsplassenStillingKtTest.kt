package no.nav.toi.stilling.publiser.arbeidsplassen

import no.nav.toi.stilling.publiser.arbeidsplassen.dto.Contact
import no.nav.toi.stilling.publiser.arbeidsplassen.dto.DirektemeldtStilling
import no.nav.toi.stilling.publiser.arbeidsplassen.dto.DirektemeldtStillingArbeidsgiver
import no.nav.toi.stilling.publiser.arbeidsplassen.dto.DirektemeldtStillingInnhold
import no.nav.toi.stilling.publiser.arbeidsplassen.dto.Geografi
import no.nav.toi.stilling.publiser.arbeidsplassen.dto.PrivacyType
import no.nav.toi.stilling.publiser.arbeidsplassen.dto.PropertyName
import org.junit.jupiter.api.Assertions.assertEquals
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
                 published = ZonedDateTime.of(LocalDateTime.of(2025, 3, 25, 14, 0), ZoneId.of("UTC")),
                 expires = ZonedDateTime.of(LocalDateTime.of(2025, 4, 25, 14, 0), ZoneId.of("UTC")),
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
                         phone = "11111111",
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
                     "engagementtype" to "Fast",
                     "sector" to "Privat",
                     "starttime" to "08.04.2025",
                     "workday" to "[\"Ukedager\"]",
                     "positioncount" to "1",
                     "adtext" to "<p>Jobb som utvikler</p>",
                 ),
             ),
             annonseId = 123,
             opprettet = ZonedDateTime.of(LocalDateTime.of(2025, 1, 25, 14, 0), ZoneId.of("UTC")),
             opprettetAv = "pam-rekrutteringsbistand",
             sistEndret = ZonedDateTime.of(LocalDateTime.of(2025, 2, 25, 14, 0), ZoneId.of("UTC")),
             sistEndretAv = "pam-rekrutteringsbistand",
             status = "ACTIVE",
        )

        val arbeidsplassenStilling = konverterTilArbeidsplassenStilling(direktemeldtStilling)

        assertEquals("123e4567-e89b-12d3-a456-426614174000", arbeidsplassenStilling.reference)
        assertEquals(1, arbeidsplassenStilling.positions)
        assertEquals("2025-03-25T14:00:00", arbeidsplassenStilling.published)
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

        assertEquals("Privat", arbeidsplassenStilling.properties[PropertyName.sector])
        assertEquals("[\"Ukedager\"]", arbeidsplassenStilling.properties[PropertyName.workday])
        assertEquals("Fast", arbeidsplassenStilling.properties[PropertyName.engagementtype])
        assertEquals("08.04.2025", arbeidsplassenStilling.properties[PropertyName.starttime])
    }
}
