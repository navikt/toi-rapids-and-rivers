package no.nav.toi.stilling.indekser

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import java.time.ZonedDateTime
import java.util.*

class DirektemeldtStillingDomeneTest {

    @Test
    fun `Skal konvertere adminstatus rejected til done`() {
        val stilling = enDirektemeldtStilling()
        val konvertertStilling = stilling.tilStilling()

        assertEquals("DONE", konvertertStilling.administration?.status)
    }

    @Test
    fun `Skal la adminstatus være pending etter konvertering`() {
        val stilling = enDirektemeldtStilling().copy(adminStatus = "PENDING")
        val konvertertStilling = stilling.tilStilling()

        assertEquals("PENDING", konvertertStilling.administration?.status)
    }

    private fun enDirektemeldtStilling() = DirektemeldtStilling(
        stillingsId = UUID.randomUUID(),
        annonsenr = "1234567",
        innhold = DirektemeldtStillingInnhold(
            title = "Teststilling",
            administration = DirektemeldtStillingAdministration(
                status = "REJECTED",
                comments = "Kommentar",
                reportee = "Navn Navnesen",
                remarks = listOf("remark1", "remark2"),
                navIdent = "Z123456"
            ),
            contactList = listOf(
                Contact(
                    name = "Ola Nordmann",
                    email = "ola@eksempel.no",
                    phone = "12345678",
                    role = "Kontaktperson",
                    title = "HR"
                )
            ),
            privacy = "INTERNAL_NOT_SHOWN",
            source = "DIR",
            medium = "DIR",
            reference = "ref-1",
            employer = DirektemeldtStillingArbeidsgiver(
                name = "Bedrift AS",
                orgnr = "999999999",
                parentOrgnr = null,
                publicName = "Bedrift AS",
                orgform = "AS"
            ),
            locationList = listOf(
                Geografi(
                    address = "Gate 1",
                    postalCode = "0123",
                    county = "Oslo",
                    municipalCode = "0301",
                    municipal = "Oslo",
                    city = "Oslo",
                    country = "Norge",
                    latitude = "59.91",
                    longitude = "10.75"
                )
            ),
            categoryList = listOf(
                DirektemeldtStillingKategori(
                    code = "1234",
                    categoryType = "STYRK08",
                    name = "Utvikler"
                )
            ),
            properties = mapOf("key1" to "value1"),
            businessName = "Bedrift AS",
            firstPublished = true,
            deactivatedByExpiry = false,
            activationOnPublishingDate = false
        ),
        opprettet = ZonedDateTime.now(),
        opprettetAv = "Z654321",
        sistEndret = ZonedDateTime.now(),
        sistEndretAv = "Z654321",
        status = "RECEIVED",
        utløpsdato = ZonedDateTime.now().plusDays(30),
        publisert = ZonedDateTime.now(),
        publisertAvAdmin = "Z654321",
        adminStatus = "REJECTED"
    )
}
