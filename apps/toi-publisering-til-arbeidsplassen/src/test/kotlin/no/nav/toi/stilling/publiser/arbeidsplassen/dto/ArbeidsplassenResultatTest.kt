package no.nav.toi.stilling.publiser.arbeidsplassen.dto

import no.nav.toi.stilling.publiser.arbeidsplassen.objectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ArbeidsplassenResultatTest {

    @Test
    fun `Deserialiseres feilmelding`() {
        val somStreng = """{"providerId" : 15054,  "status" : "ERROR",  "message" : "Invalid value: 2025-03-20 at published",  "md5" : "",  "items" : 1,  "created" : "2025-03-20T13:51:32.193613915",  "updated" : "2025-03-20T13:51:32.193617235"}"""
        val readValue = objectMapper.readValue(somStreng, ArbeidsplassenResultat::class.java)

        assertEquals(15054, readValue.providerId)
        assertEquals("ERROR", readValue.status)
        assertEquals("Invalid value: 2025-03-20 at published", readValue.message)
        assertEquals("", readValue.md5)
        assertEquals(1, readValue.items)
        assertEquals("2025-03-20T13:51:32.193613915", readValue.created)
        assertEquals("2025-03-20T13:51:32.193617235", readValue.updated)
    }

    @Test
    fun `Deserialiseres ok melding`() {
        val somStreng = """{"versionId" : 584110,  "providerId" : 15054,  "status" : "RECEIVED",  "md5" : "16F17D0782E45BC35650164C1B8D0D8D",  "items" : 1,  "created" : "2025-03-20T14:49:00.317605648",  "updated" : "2025-03-20T14:49:00.317606488"}"""
        val readValue = objectMapper.readValue(somStreng, ArbeidsplassenResultat::class.java)

        assertEquals(15054, readValue.providerId)
        assertEquals("RECEIVED", readValue.status)
        assertNull(readValue.message)
        assertEquals("16F17D0782E45BC35650164C1B8D0D8D", readValue.md5)
        assertEquals(1, readValue.items)
        assertEquals("2025-03-20T14:49:00.317605648", readValue.created)
        assertEquals("2025-03-20T14:49:00.317606488", readValue.updated)
    }
}
