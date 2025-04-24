package no.nav.arbeidsgiver.toi.siste14avedtak

import no.nav.toi.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class Siste14aVedtakTest {

    @Test
    fun `Lesing av siste14aVedtak fra eksternt topic skal produsere ny melding på rapid`() {
        val testRapid = TestRapid()
        val aktørid = "123"

        Siste14aVedtakLytter(testRapid)

        testRapid.sendTestMessage(siste14aVedtakMeldingFraEksterntTopic(aktørid))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
        val meldingJson = inspektør.message(0)

        assertThat(meldingJson.fieldNames().asSequence().toList()).containsExactlyInAnyOrder(
            "@event_name",
            "aktørId",
            "siste14avedtak",
            "system_read_count",
            "@id",
            "@opprettet",
            "system_participating_services"
        )

        assertThat(meldingJson.get("@event_name").asText()).isEqualTo("siste14avedtak")
        assertThat(meldingJson.get("aktørId").asText()).isEqualTo(aktørid)

        val json = meldingJson.get("siste14avedtak")
        assertThat(json.fieldNames().asSequence().toList()).containsExactlyInAnyOrder(
            "aktorId",
            "innsatsgruppe",
            "hovedmal",
            "fattetDato",
            "fraArena"
        )

        meldingJson.get("siste14avedtak").apply {
            assertThat(get("aktorId").asText()).isEqualTo(aktørid)
            assertThat(get("innsatsgruppe").asText()).isEqualTo("STANDARD_INNSATS")
            assertThat(get("hovedmal").asText()).isEqualTo("SKAFFE_ARBEID")
            assertThat(get("fattetDato").asText()).isEqualTo("2021-09-08T09:29:20.398043+02:00")
            assertThat(get("fraArena").asText()).isEqualTo("false")


        }
    }
    private fun siste14aVedtakMeldingFraEksterntTopic(aktørId: String) = """
        {
          "aktorId": "$aktørId",
          "innsatsgruppe": "STANDARD_INNSATS",
          "hovedmal": "SKAFFE_ARBEID",
          "fattetDato": "2021-09-08T09:29:20.398043+02:00",
          "fraArena": false
        }
    """.trimIndent()


}
