package no.nav.arbeidsgiver.toi.kvp

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KvpTest {

    @Test
    fun `Lesing av kvp-melding fra eksternt topic skal produsere ny melding på rapid`() {
        val testRapid = TestRapid()
        val aktørId = "123"

        KvpLytter(testRapid)

        val melding = kvpFraEksterntTopic(aktørId)

        testRapid.sendTestMessage(melding)

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
            "event",
            "aktorId",
            "enhetId",
            "startet",
            "avsluttet"
        )

        meldingJson.get("kvp").apply {
            assertThat(get("event").asText()).isEqualTo("AVSLUTTET")
            assertThat(get("aktorId").asText()).isEqualTo(aktørId)
            assertThat(get("enhetId").asText()).isEqualTo("0123")
            assertThat(get("startet").get("opprettetDato").asText()).isEqualTo("2023-01-03T09:44:23.394628+01:00")
            assertThat(get("avsluttet").get("avsluttetDato").asText()).isEqualTo("2023-01-03T09:44:48.891877+01:00")
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
            ${if (eventName) """"@event_name":"kvp",""" else ""}
            "event": "AVSLUTTET",
            "aktorId": "$aktørId",
            "enhetId": "0123",
             "startet": {
                "opprettetAv": "Z100000",
                "opprettetDato": "2023-01-03T09:44:23.394628+01:00",
                "opprettetBegrunnelse": "vzcfv"
              },
              "avsluttet": {
                "avsluttetAv": "Z100000",
                "avsluttetDato": "2023-01-03T09:44:48.891877+01:00",
                "avsluttetBegrunnelse": "dczxd"
              }  
        }
    """.trimIndent()
}

