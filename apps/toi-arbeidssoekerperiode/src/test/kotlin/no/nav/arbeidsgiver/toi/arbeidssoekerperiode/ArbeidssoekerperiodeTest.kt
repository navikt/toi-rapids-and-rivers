package no.nav.arbeidsgiver.toi.arbeidssoekerperiode

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ArbeidssoekerperiodeTest {

    @Test
    fun `Lesing av arbeidssoekerperiodeMelding fra eksternt topic skal foreløpig ikke produsere ny melding på rapid`() {
        val testRapid = TestRapid()
        val aktørId = "100000001"

        ArbeidssoekerperiodeLytter(testRapid)

        testRapid.sendTestMessage(arbeidssoekerperiodeMeldingFraEksterntTopic(aktørId))
        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(0)
        /*
        val meldingJson = inspektør.message(0)

        assertThat(meldingJson.fieldNames().asSequence().toList()).containsExactlyInAnyOrder(
            "@event_name",
            "arbeidssoekerperiode",
            "aktørId",
            "system_read_count",
            "@id",
            "@opprettet",
            "system_participating_services"
        )

        assertThat(meldingJson.get("@event_name").asText()).isEqualTo("arbeidssoekerperiode")
        assertThat(meldingJson.get("aktørId").asText()).isEqualTo(aktørId)

        val arbeidssoekerperiodeJson = meldingJson.get("arbeidssoekerperiode")
        assertThat(arbeidssoekerperiodeJson.fieldNames().asSequence().toList()).containsExactlyInAnyOrder(
            "uuid",
            "aktorId",
            "startDato",
            "sluttDato"
        )

        arbeidssoekerperiodeJson.apply {
            assertThat(get("uuid").asText()).isEqualTo("0b0e2261-343d-488e-a70f-807f4b151a2f")
            assertThat(get("aktorId").asText()).isEqualTo(aktørId)
            assertThat(get("startDato").asText()).isEqualTo("2020-10-30T14:15:38+01:00")
            assertThat(get("sluttDato").isNull).isTrue
        }

         */
    }

    private fun arbeidssoekerperiodeMeldingFraEksterntTopic(aktørId: String) = """
        {
            "uuid": "0b0e2261-343d-488e-a70f-807f4b151a2f",
            "aktorId": "${aktørId}",
            "startDato": "2020-10-30T14:15:38+01:00",
            "sluttDato": null 
        }
    """.trimIndent()
}
