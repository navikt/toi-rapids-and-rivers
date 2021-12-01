package no.nav.arbeidsgiver.toi.oppfolgingsinformasjon

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OppfolgingsinformasjonTest {

    @Test
    fun `Lesing av oppfølgingsinformasjonMelding fra eksternt topic skal produsere ny melding på rapid`() {
        val testRapid = TestRapid()
        OppfolgingsinformasjonLytter(testRapid)

        testRapid.sendTestMessage(oppfølgingsinformasjonMeldingFraEksterntTopic())
        Thread.sleep(300)

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
        val meldingJson = inspektør.message(0)

        assertThat(meldingJson.fieldNames().asSequence().toList()).containsExactlyInAnyOrder(
            "@event_name",
            "oppfølgingsinformasjon",
            "system_read_count"
        )
        assertThat(meldingJson.get("@event_name").asText()).isEqualTo("oppfølgingsinformasjon")

        val oppfølgingsinformasjonJson = meldingJson.get("oppfølgingsinformasjon")
        assertThat(oppfølgingsinformasjonJson.fieldNames().asSequence().toList()).containsExactlyInAnyOrder(
            "fodselsnummer",
            "formidlingsgruppe",
            "iservFraDato",
            "fornavn",
            "etternavn",
            "oppfolgingsenhet",
            "kvalifiseringsgruppe",
            "rettighetsgruppe",
            "hovedmaal",
            "sikkerhetstiltakType",
            "diskresjonskode",
            "harOppfolgingssak",
            "sperretAnsatt",
            "erDoed",
            "doedFraDato",
            "sistEndretDato"
        )
        meldingJson.get("oppfølgingsinformasjon").apply {
            assertThat(get("fodselsnummer").asText()).isEqualTo("12345678912")
            assertThat(get("formidlingsgruppe").asText()).isEqualTo("IARBS")
            assertThat(get("iservFraDato").isNull).isTrue
            assertThat(get("fornavn").asText()).isEqualTo("TULLETE")
            assertThat(get("etternavn").asText()).isEqualTo("TABBE")
            assertThat(get("oppfolgingsenhet").asText()).isEqualTo("0318")
            assertThat(get("kvalifiseringsgruppe").asText()).isEqualTo("BATT")
            assertThat(get("rettighetsgruppe").asText()).isEqualTo("AAP")
            assertThat(get("hovedmaal").asText()).isEqualTo("BEHOLDEA")
            assertThat(get("sikkerhetstiltakType").isNull).isTrue
            assertThat(get("diskresjonskode").isNull).isTrue
            assertThat(get("harOppfolgingssak").asBoolean()).isTrue
            assertThat(get("sperretAnsatt").asBoolean()).isFalse
            assertThat(get("erDoed").asBoolean()).isFalse
            assertThat(get("doedFraDato").isNull).isTrue
            assertThat(get("sistEndretDato").asText()).isEqualTo("2020-10-30T14:15:38+01:00")
        }
    }

    private fun oppfølgingsinformasjonMeldingFraEksterntTopic() = """
        {
            "fodselsnummer": "12345678912",
            "formidlingsgruppe": "IARBS",
            "iservFraDato": null,
            "fornavn": "TULLETE",
            "etternavn": "TABBE",
            "oppfolgingsenhet": "0318",
            "kvalifiseringsgruppe": "BATT",
            "rettighetsgruppe": "AAP",
            "hovedmaal": "BEHOLDEA",
            "sikkerhetstiltakType": null,
            "diskresjonskode": null,
            "harOppfolgingssak": true,
            "sperretAnsatt": false,
            "erDoed": false,
            "doedFraDato": null,
            "sistEndretDato": "2020-10-30T14:15:38+01:00"
        }
    """.trimIndent()
}
