package no.nav.arbeidsgiver.toi.kvp

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KvpTest {

    @Test
    fun `Lesing av kvp-opprettet-melding fra eksternt topic skal produsere ny melding på rapid`() {
        val testRapid = TestRapid()
        val aktørId = "123"

        StartetLytter(testRapid)
        AvsluttetLytter(testRapid)

        testRapid.sendTestMessage(kvpOpprettetFraEksterntTopic(aktørId))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
        val meldingJson = inspektør.message(0)

        assertThat(meldingJson.fieldNames().asSequence().toList()).containsExactlyInAnyOrder(
            "@event_name",
            "kvpOpprettet",
            "aktørId",
            "system_read_count",
            "@id",
            "@opprettet",
            "system_participating_services"
        )

        assertThat(meldingJson.get("@event_name").asText()).isEqualTo("kvp-opprettet")
        assertThat(meldingJson.get("aktørId").asText()).isEqualTo(aktørId)

        val kvpOpprettet = meldingJson.get("kvpOpprettet")
        assertThat(kvpOpprettet.fieldNames().asSequence().toList()).containsExactlyInAnyOrder(
            "aktorId",
            "enhetId",
            "opprettetAv",
            "opprettetDato",
            "opprettetBegrunnelse",
        )

        meldingJson.get("kvpOpprettet").apply {
            assertThat(get("aktorId").asText()).isEqualTo(aktørId)
            assertThat(get("enhetId").asText()).isEqualTo("0123")
            assertThat(get("opprettetAv").asText()).isEqualTo("A100000")
            assertThat(get("opprettetDato").asText()).isEqualTo("2020-10-30")
            assertThat(get("opprettetBegrunnelse").asText()).isEqualTo("Derfor")
        }
    }


    @Test
    fun `Lesing av kvp-avsluttet-melding fra eksternt topic skal produsere ny melding på rapid`() {
        val testRapid = TestRapid()
        val aktørId = "123"

        StartetLytter(testRapid)
        AvsluttetLytter(testRapid)

        testRapid.sendTestMessage(kvpAvsluttetFraEksterntTopic(aktørId))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
        val meldingJson = inspektør.message(0)

        assertThat(meldingJson.fieldNames().asSequence().toList()).containsExactlyInAnyOrder(
            "@event_name",
            "kvpAvsluttet",
            "aktørId",
            "system_read_count",
            "@id",
            "@opprettet",
            "system_participating_services"
        )

        assertThat(meldingJson.get("@event_name").asText()).isEqualTo("kvp-avsluttet")
        assertThat(meldingJson.get("aktørId").asText()).isEqualTo(aktørId)

        val kvpOpprettet = meldingJson.get("kvpAvsluttet")
        assertThat(kvpOpprettet.fieldNames().asSequence().toList()).containsExactlyInAnyOrder(
            "aktorId",
            "avsluttetAv",
            "avsluttetDato",
            "avsluttetBegrunnelse",
        )

        meldingJson.get("kvpAvsluttet").apply {
            assertThat(get("aktorId").asText()).isEqualTo(aktørId)
            assertThat(get("avsluttetAv").asText()).isEqualTo("A100001")
            assertThat(get("avsluttetDato").asText()).isEqualTo("2020-10-31")
            assertThat(get("avsluttetBegrunnelse").asText()).isEqualTo("Tabbe")
        }
    }

    @Test
    fun `Ikke reager på meldinger der event_name er satt`() {
        val testRapid = TestRapid()
        val aktørId = "123"

        StartetLytter(testRapid)
        AvsluttetLytter(testRapid)

        testRapid.sendTestMessage(kvpOpprettetFraEksterntTopic(aktørId, true))
        testRapid.sendTestMessage(kvpAvsluttetFraEksterntTopic(aktørId, true))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(0)
    }

    private fun kvpOpprettetFraEksterntTopic(aktørId: String, eventName: Boolean = false) = """
        {
            ${if(eventName) """"@event_name":"kvp-opprettet",""" else ""}
            "aktorId": "$aktørId",
            "enhetId": "0123",
            "opprettetAv": "A100000",
            "opprettetDato": "2020-10-30",
            "opprettetBegrunnelse": "Derfor"
        }
    """.trimIndent()

    private fun kvpAvsluttetFraEksterntTopic(aktørId: String, eventName: Boolean = false) = """
        {
            ${if(eventName) """"@event_name":"kvp-avsluttet",""" else ""}
            "aktorId": "$aktørId",
            "avsluttetAv": "A100001",
            "avsluttetDato": "2020-10-31",
            "avsluttetBegrunnelse": "Tabbe"
        }
    """.trimIndent()
}
