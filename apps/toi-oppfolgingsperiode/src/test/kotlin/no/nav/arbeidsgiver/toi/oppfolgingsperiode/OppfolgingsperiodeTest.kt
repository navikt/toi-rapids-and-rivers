package no.nav.arbeidsgiver.toi.oppfolgingsperiode

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OppfolgingsperiodeTest {

    @Test
    fun `Lesing av oppfølgingsperiodeMelding fra eksternt topic skal produsere ny melding på rapid`() {
        val testRapid = TestRapid()
        val aktørId = "100000001"

        OppfolgingsperiodeLytter(testRapid)

        testRapid.sendTestMessage(oppfølgingsperiodeMeldingFraEksterntTopic(aktørId))
        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
        val meldingJson = inspektør.message(0)

        assertThat(meldingJson.fieldNames().asSequence().toList()).containsExactlyInAnyOrder(
            "@event_name",
            "oppfølgingsperiode",
            "aktørId",
            "system_read_count",
            "@id",
            "@opprettet",
            "system_participating_services"
        )

        assertThat(meldingJson.get("@event_name").asText()).isEqualTo("oppfølgingsperiode")
        assertThat(meldingJson.get("aktørId").asText()).isEqualTo(aktørId)

        val oppfølgingsperiodeJson = meldingJson.get("oppfølgingsperiode")
        assertThat(oppfølgingsperiodeJson.fieldNames().asSequence().toList()).containsExactlyInAnyOrder(
            "uuid",
            "aktorId",
            "startDato",
            "sluttDato"
        )

        oppfølgingsperiodeJson.apply {
            assertThat(get("uuid").asText()).isEqualTo("0b0e2261-343d-488e-a70f-807f4b151a2f")
            assertThat(get("aktorId").asText()).isEqualTo(aktørId)
            assertThat(get("startDato").asText()).isEqualTo("2020-10-30T14:15:38+01:00")
            assertThat(get("sluttDato").isNull).isTrue
        }
    }

    private fun oppfølgingsperiodeMeldingFraEksterntTopic(aktørId: String) = """
        {
            "uuid": "0b0e2261-343d-488e-a70f-807f4b151a2f",
            "aktorId": "${aktørId}",
            "startDato": "2020-10-30T14:15:38+01:00",
            "sluttDato": null 
        }
    """.trimIndent()
}
