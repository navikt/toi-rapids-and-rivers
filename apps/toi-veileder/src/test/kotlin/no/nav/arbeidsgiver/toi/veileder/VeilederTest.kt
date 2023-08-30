package no.nav.arbeidsgiver.toi.veileder

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class VeilederTest {

    @Test
    fun `Lesing av veilederMelding fra eksternt topic skal produsere ny melding på rapid`() {
        val aktørId = "10000100000"
        val veilederId = "2000020000"
        val tilordnet = "2020-12-21T10:58:19.023+01:00"
        val testRapid = TestRapid()
        VeilederLytter(testRapid, nomKlient)

        testRapid.sendTestMessage(veilederMeldingFraEksterntTopic(aktørId, veilederId, tilordnet))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
        val meldingJson = inspektør.message(0)

        assertThat(meldingJson.fieldNames().asSequence().toList()).containsExactlyInAnyOrder(
            "@event_name",
            "veileder",
            "aktørId",
            "system_read_count",
            "@id",
            "@opprettet",
            "system_participating_services"
        )
        assertThat(meldingJson.get("@event_name").asText()).isEqualTo("veileder")
        assertThat(meldingJson.get("aktørId").asText()).isEqualTo(aktørId)

        val veilederJson = meldingJson.get("veileder")
        assertThat(veilederJson.fieldNames().asSequence().toList()).containsExactlyInAnyOrder(
            "aktorId",
            "veilederId",
            "tilordnet",
        )
        meldingJson.get("veileder").apply {
            assertThat(get("aktorId").asText()).isEqualTo(aktørId)
            assertThat(get("veilederId").asText()).isEqualTo(veilederId)
            assertThat(get("tilordnet").asText()).isEqualTo(tilordnet)
        }
    }

    private fun veilederMeldingFraEksterntTopic(aktørId: String, veilederId: String, tilordnet: String) = """
        {
            "aktorId":"$aktørId",
            "veilederId":"$veilederId",
            "tilordnet":"$tilordnet"
        }
    """.trimIndent()
}