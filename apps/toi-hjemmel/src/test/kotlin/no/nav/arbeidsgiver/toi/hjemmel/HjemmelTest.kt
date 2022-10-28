package no.nav.arbeidsgiver.toi.hjemmel

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HjemmelTest {

    @Test
    fun `Lesing av hjemmelMelding fra eksternt topic skal produsere ny melding på rapid`() {
        val aktørId = "10000100000"
        val testRapid = TestRapid()
        HjemmelLytter(testRapid)

        val hjemmelMeldingFraEksterntTopic = hjemmelMeldingFraEksterntTopic(aktørId, "CV_HJEMMEL")

        testRapid.sendTestMessage(hjemmelMeldingFraEksterntTopic)

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
        val meldingJson = inspektør.message(0)

        assertThat(meldingJson.fieldNames().asSequence().toList()).containsExactlyInAnyOrder(
            "@event_name",
            "hjemmel",
            "aktørId",
            "system_read_count",
            "@id",
            "@opprettet",
            "system_participating_services"
        )
        assertThat(meldingJson.get("@event_name").asText()).isEqualTo("hjemmel")
        assertThat(meldingJson.get("aktørId").asText()).isEqualTo(aktørId)

        val hjemmelJson = meldingJson.get("hjemmel")
        assertThat(hjemmelJson.fieldNames().asSequence().toList()).containsExactlyInAnyOrder(
            "samtykkeId",
            "aktoerId",
            "fnr",
            "ressurs",
            "opprettetDato",
            "slettetDato",
            "versjon",
            "versjonGjeldendeFra",
            "versjonGjeldendeTil"
        )
        assertThat(hjemmelJson.toPrettyString()).isEqualTo(hjemmelMeldingFraEksterntTopic)
    }

    @Test
    fun `Lesing av samtykkemelding som ikke er hjemmel skal ikke produsere ny melding på rapid`() {
        val aktørId = "10000100000"
        val testRapid = TestRapid()
        HjemmelLytter(testRapid)

        val hjemmelMeldingFraEksterntTopic = hjemmelMeldingFraEksterntTopic(aktørId, "CV_GENERELL")

        testRapid.sendTestMessage(hjemmelMeldingFraEksterntTopic)

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(0)
    }

    private fun hjemmelMeldingFraEksterntTopic(aktørId: String, ressurs: String) = """
        {
          "samtykkeId" : 1,
          "aktoerId" : "AktorId(aktorId=$aktørId)",
          "fnr" : "27075349594",
          "ressurs" : "$ressurs",
          "opprettetDato" : "2019-01-09T12:36:06+01:00",
          "slettetDato" : null,
          "versjon" : 1,
          "versjonGjeldendeFra" : null,
          "versjonGjeldendeTil" : "2019-04-08"
        }
    """.trimIndent()
}
