package no.nav.arbeidsgiver.toi.samtykke

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SamtykkeTest {

    @Test
    fun `Lesing av samtykkeMelding fra eksternt topic skal produsere ny melding på rapid`() {
        val aktørId = "10000100000"
        val testRapid = TestRapid()
        SamtykkeLytter(testRapid)

        testRapid.sendTestMessage(samtykkeMeldingFraEksterntTopic(aktørId))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
        val meldingJson = inspektør.message(0)

        assertThat(meldingJson.fieldNames().asSequence().toList()).containsExactlyInAnyOrder(
            "@event_name",
            "samtykke",
            "aktørId",
            "system_read_count"
        )
        assertThat(meldingJson.get("@event_name").asText()).isEqualTo("samtykke")
        assertThat(meldingJson.get("aktørId").asText()).isEqualTo(aktørId)

        val samtykkeJson = meldingJson.get("samtykke")
        assertThat(samtykkeJson.fieldNames().asSequence().toList()).containsExactlyInAnyOrder(
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
        assertThat(samtykkeJson.toPrettyString()).isEqualTo(samtykkeMeldingFraEksterntTopic(aktørId))
    }

    private fun samtykkeMeldingFraEksterntTopic(aktørId: String) = """
        {
          "samtykkeId" : 1,
          "aktoerId" : "AktorId(aktorId=$aktørId)",
          "fnr" : "27075349594",
          "ressurs" : "CV_HJEMMEL",
          "opprettetDato" : "2019-01-09T12:36:06+01:00",
          "slettetDato" : null,
          "versjon" : 1,
          "versjonGjeldendeFra" : null,
          "versjonGjeldendeTil" : "2019-04-08"
        }
    """.trimIndent()
}