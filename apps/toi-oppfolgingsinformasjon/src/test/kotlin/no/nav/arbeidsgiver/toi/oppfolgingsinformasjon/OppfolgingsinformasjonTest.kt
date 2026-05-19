package no.nav.arbeidsgiver.toi.oppfolgingsinformasjon

import no.nav.toi.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OppfolgingsinformasjonTest {

    @Test
    fun `Lesing av oppfølgingsinformasjonMelding fra eksternt topic skal produsere ny melding på rapid`() {
        val testRapid = TestRapid()
        val fødselsnummer = "123"

        OppfolgingsinformasjonLytter(testRapid)

        testRapid.sendTestMessage(oppfølgingsinformasjonMeldingFraEksterntTopic(fødselsnummer))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
        val meldingJson = inspektør.message(0)

        assertThat(meldingJson.propertyNames()).containsExactlyInAnyOrder(
            "@event_name",
            "oppfølgingsinformasjon",
            "fodselsnummer",
            "system_read_count",
            "@id",
            "@opprettet",
            "system_participating_services"
        )

        assertThat(meldingJson.get("@event_name").asString()).isEqualTo("oppfølgingsinformasjon")
        assertThat(meldingJson.get("fodselsnummer").asString()).isEqualTo(fødselsnummer)

        val oppfølgingsinformasjonJson = meldingJson.get("oppfølgingsinformasjon")
        assertThat(oppfølgingsinformasjonJson.propertyNames()).containsExactlyInAnyOrder(
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
            assertThat(get("fodselsnummer").asString()).isEqualTo(fødselsnummer)
            assertThat(get("formidlingsgruppe").asString()).isEqualTo("ARBS")
            assertThat(get("iservFraDato").isNull).isTrue
            assertThat(get("fornavn").asString()).isEqualTo("TULLETE")
            assertThat(get("etternavn").asString()).isEqualTo("TABBE")
            assertThat(get("oppfolgingsenhet").asString()).isEqualTo("0318")
            assertThat(get("kvalifiseringsgruppe").asString()).isEqualTo("BATT")
            assertThat(get("rettighetsgruppe").asString()).isEqualTo("AAP")
            assertThat(get("hovedmaal").asString()).isEqualTo("BEHOLDEA")
            assertThat(get("sikkerhetstiltakType").isNull).isTrue
            assertThat(get("diskresjonskode").isNull).isTrue
            assertThat(get("harOppfolgingssak").asBoolean()).isTrue
            assertThat(get("sperretAnsatt").asBoolean()).isFalse
            assertThat(get("erDoed").asBoolean()).isFalse
            assertThat(get("doedFraDato").isNull).isTrue
            assertThat(get("sistEndretDato").asString()).isEqualTo("2020-10-30T14:15:38+01:00")
        }
    }

    private fun oppfølgingsinformasjonMeldingFraEksterntTopic(fødselsnummer: String) = """
        {
            "fodselsnummer": "$fødselsnummer",
            "formidlingsgruppe": "ARBS",
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
