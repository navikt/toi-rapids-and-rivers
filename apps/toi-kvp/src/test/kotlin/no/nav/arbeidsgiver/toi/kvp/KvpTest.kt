package no.nav.arbeidsgiver.toi.kvp

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KvpTest {

    @Test
    fun `Lesing av kvp-melding fra eksternt topic skal produsere ny melding på rapid`() {
        val testRapid = TestRapid()
        val aktørId = "123"

        KvpLytter(testRapid)

        testRapid.sendTestMessage(kvpFraEksterntTopic(aktørId))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
        val meldingJson = inspektør.message(0)

        assertThat(meldingJson.fieldNames().asSequence().toList()).containsExactlyInAnyOrder(
            "@event_name",
            "kvp",
            "aktørId",
            "system_read_count",
            "@id",
            "@opprettet",
            "system_participating_services"
        )

        assertThat(meldingJson.get("@event_name").asText()).isEqualTo("kvp")
        assertThat(meldingJson.get("aktørId").asText()).isEqualTo(aktørId)

        val kvp = meldingJson.get("kvp")
        assertThat(kvp.fieldNames().asSequence().toList()).containsExactlyInAnyOrder(
            "aktorId",
            "enhetId",
            "endretAv",
            "opprettetDato",
            "avsluttetDato",
        )

        meldingJson.get("kvp").apply {
            assertThat(get("aktorId").asText()).isEqualTo(aktørId)
            assertThat(get("enhetId").asText()).isEqualTo("0123")
            assertThat(get("endretAv").asText()).isEqualTo("A100000")
            assertThat(get("opprettetDato").asText()).isEqualTo("2020-10-30")
            assertThat(get("avsluttetDato").asText()).isEqualTo("2022-10-30")
        }
    }

    @Test
    fun `Ikke reager på meldinger der event_name er satt`() {
        val testRapid = TestRapid()
        val aktørId = "123"

        KvpLytter(testRapid)

        testRapid.sendTestMessage(kvpFraEksterntTopic(aktørId, true))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(0)
    }

    private fun kvpFraEksterntTopic(aktørId: String, eventName: Boolean = false) = """
        {
            ${if(eventName) """"@event_name":"kvp",""" else ""}
            "aktorId": "$aktørId",
            "enhetId": "0123",
            "endretAv": "A100000",
            "opprettetDato": "2020-10-30",
            "avsluttetDato": "2022-10-30"
        }
    """.trimIndent()
}
