package no.nav.arbeidsgiver.toi.oppfolgingsperiode

import no.nav.toi.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OppfolgingsperiodeTest {

    @Test
    fun `Lesing av oppfølgingsperiodeMelding fra eksternt topic skal produsere ny melding på rapid`() {
        val testRapid = TestRapid()
        val aktørId = "100000001"

        SisteOppfolgingsperiodeLytter(testRapid)

        testRapid.sendTestMessage(oppfølgingsperiodeMeldingFraEksterntTopic(aktørId))
        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
        val meldingJson = inspektør.message(0)

        assertThat(meldingJson.fieldNames().asSequence().toList()).containsExactlyInAnyOrder(
            "@event_name",
            "sisteOppfølgingsperiode",
            "aktørId",
            "system_read_count",
            "@id",
            "@opprettet",
            "system_participating_services"
        )

        assertThat(meldingJson.get("@event_name").asText()).isEqualTo("sisteOppfølgingsperiode")
        assertThat(meldingJson.get("aktørId").asText()).isEqualTo(aktørId)

        val oppfølgingsperiodeJson = meldingJson.get("sisteOppfølgingsperiode")
        assertThat(oppfølgingsperiodeJson.fieldNames().asSequence().toList()).containsExactlyInAnyOrder(
            "kontor",
            "aktorId",
            "startTidspunkt",
            "sluttTidspunkt",
            "ident",
            "sisteEndringsType",
            "oppfolgingsperiodeUuid",
            "producerTimestamp"
        )

        oppfølgingsperiodeJson.apply {
            assertThat(get("kontor").get("kontorNavn").asText()).isEqualTo("Nav Asker")
            assertThat(get("kontor").get("kontorId").asText()).isEqualTo("0220")
            assertThat(get("aktorId").asText()).isEqualTo(aktørId)
            assertThat(get("startTidspunkt").asText()).isEqualTo("2026-01-01T03:01:07.024557+01:00")
            assertThat(get("sluttTidspunkt").isNull).isTrue
            assertThat(get("ident").asText()).isEqualTo("01234567890")
            assertThat(get("sisteEndringsType").asText()).isEqualTo("OPPFOLGING_STARTET")
            assertThat(get("oppfolgingsperiodeUuid").asText()).isEqualTo("11b016d9-37a0-4c5d-8296-62ccf90f9745")
            assertThat(get("producerTimestamp").asText()).isEqualTo("2026-01-01T03:01:08.921369321+01:00")
        }
    }

    private fun oppfølgingsperiodeMeldingFraEksterntTopic(aktørId: String) = """
        {
            "startTidspunkt":"2026-01-01T03:01:07.024557+01:00",
            "kontor": {
                "kontorNavn":"Nav Asker",
                "kontorId":"0220"
            },
            "aktorId":"$aktørId",
            "ident":"01234567890",
            "sisteEndringsType":"OPPFOLGING_STARTET",
            "oppfolgingsperiodeUuid":"11b016d9-37a0-4c5d-8296-62ccf90f9745",
            "sluttTidspunkt":null,
            "producerTimestamp":"2026-01-01T03:01:08.921369321+01:00"
        }
    """.trimIndent()
}
