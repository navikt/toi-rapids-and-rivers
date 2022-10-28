package no.nav.arbeidsgiver.toi.maaBehandleTidligereCv

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MaaBehandleTidligereCvTest {

    @Test
    fun `Lesing av måBehandleTidligereCv-melding fra eksternt topic skal produsere ny melding på rapid`() {
        val aktørId = "10000100000"
        val testRapid = TestRapid()
        MaaBehandleTidligereCvLytter(testRapid)

        val måBehandleTidligereCvMeldingFraEksterntTopic = måBehandleTidligereCvMeldingFraEksterntTopic(aktørId, true)

        testRapid.sendTestMessage(måBehandleTidligereCvMeldingFraEksterntTopic)

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
        val meldingJson = inspektør.message(0)

        assertThat(meldingJson.fieldNames().asSequence().toList()).containsExactlyInAnyOrder(
            "@event_name",
            "måBehandleTidligereCv",
            "aktørId",
            "system_read_count",
            "@id",
            "@opprettet",
            "system_participating_services"
        )
        assertThat(meldingJson.get("@event_name").asText()).isEqualTo("må-behandle-tidligere-cv")
        assertThat(meldingJson.get("aktørId").asText()).isEqualTo(aktørId)
        val måBehandleTidligereCvMelding = meldingJson.get("måBehandleTidligereCv")

        assertThat(måBehandleTidligereCvMelding.fieldNames().asSequence().toList()).containsExactlyInAnyOrder(
            "aktorId",
            "maaBehandleTidligereCv"
        )
        assertThat(måBehandleTidligereCvMelding.get("aktorId").asText()).isEqualTo(aktørId)
        assertThat(måBehandleTidligereCvMelding.get("maaBehandleTidligereCv").asBoolean()).isEqualTo(true)
    }

    @Test
    fun `Lesing av måBehandleTidligereCv-melding fra eksternt topic med verdi false skal produsere ny melding på rapid der verdi også er false`() {
        val aktørId = "10000100000"
        val testRapid = TestRapid()
        MaaBehandleTidligereCvLytter(testRapid)

        val måBehandleTidligereCvMeldingFraEksterntTopic = måBehandleTidligereCvMeldingFraEksterntTopic(aktørId, false)
        testRapid.sendTestMessage(måBehandleTidligereCvMeldingFraEksterntTopic)

        val inspektør = testRapid.inspektør
        val måBehandleTidligereCvMelding =  inspektør.message(0).get("måBehandleTidligereCv")
        assertThat(måBehandleTidligereCvMelding.get("aktorId").asText()).isEqualTo(aktørId)
        assertThat(måBehandleTidligereCvMelding.get("maaBehandleTidligereCv").asBoolean()).isEqualTo(false)
    }

    private fun måBehandleTidligereCvMeldingFraEksterntTopic(aktørId: String, maaBehandleTidligereCv: Boolean) = """
        {
          "aktorId" : "$aktørId",
          "maaBehandleTidligereCv": $maaBehandleTidligereCv
        }
    """.trimIndent()
}
