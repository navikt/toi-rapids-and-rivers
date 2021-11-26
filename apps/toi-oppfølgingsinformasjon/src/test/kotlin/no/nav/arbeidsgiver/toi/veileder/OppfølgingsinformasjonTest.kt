package no.nav.arbeidsgiver.toi.veileder

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OppfølgingsinformasjonTest {

    @Test
    fun `Lesing av oppfølgingsionformasjonMelding fra eksternt topic skal produsere ny melding på rapid`() {
        val aktørId = "10000100000"
        val testRapid = TestRapid()
        VeilederLytter(testRapid)

        testRapid.sendTestMessage(veilederMeldingFraEksterntTopic(aktørId))
        Thread.sleep(300)

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
        val meldingJson = inspektør.message(0)

        assertThat(meldingJson.fieldNames().asSequence().toList()).containsExactlyInAnyOrder(
            "@event_name",
            "oppfølgingsinformasjon",
            "aktørId",
            "system_read_count"

        )
        assertThat(meldingJson.get("@event_name").asText()).isEqualTo("oppfølgingsinformasjon")
        assertThat(meldingJson.get("aktørId").asText()).isEqualTo(aktørId)

        val veilederJson = meldingJson.get("oppfølgingsinformasjon")
        assertThat(veilederJson.fieldNames().asSequence().toList()).containsExactlyInAnyOrder(
                "aktoerid",
                "fodselsnr",
                "formidlingsgruppekode",
                "iserv_fra_dato",
                "etternavn",
                "fornavn",
                "nav_kontor",
                "kvalifiseringsgruppekode",
                "rettighetsgruppekode",
                "hovedmaalkode",
                "sikkerhetstiltak_type_kode",
                "fr_kode",
                "har_oppfolgingssak",
                "sperret_ansatt",
                "er_doed",
                "doed_fra_dato",
                "endret_dato",
        )
        meldingJson.get("veileder").apply {
            assertThat(get("aktoerid").asText()).isEqualTo(aktørId)
            assertThat(get("fodselsnr").asText()).isEqualTo("12312312")
            assertThat(get("iserv_fra_dato").asText()).isEqualTo("2020-12-21T10:58:19.023+01:00")
        }
    }

    private fun veilederMeldingFraEksterntTopic(aktørId: String) = """
        {
            "aktoerid":"$aktørId",
            "fodselsnr":"12312312312",
            "formidlingsgruppekode":"1",
            "iserv_fra_dato":"2020-12-21T10:58:19.023+01:00",
            "etternavn":"Normann",
            "fornavn":"Ola",
            "nav_kontor":"123",
            "kvalifiseringsgruppekode":"1",
            "rettighetsgruppekode":"2",
            "hovedmaalkode":"3",
            "sikkerhetstiltak_type_kode":"4",
            "fr_kode":"5",
            "har_oppfolgingssak":"ja",
            "sperret_ansatt":"nei",
            "er_doed":"nei",
            "doed_fra_dato":"2020-12-21T10:58:19.023+01:00",
            "endret_dato":"2020-12-21T10:58:19.023+01:00"
        }
    """.trimIndent()
}