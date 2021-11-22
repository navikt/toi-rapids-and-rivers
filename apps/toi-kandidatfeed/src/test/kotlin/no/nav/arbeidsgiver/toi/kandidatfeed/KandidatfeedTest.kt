package no.nav.arbeidsgiver.toi.kandidatfeed

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.apache.kafka.clients.producer.MockProducer
import org.apache.kafka.common.serialization.StringSerializer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KandidatfeedTest {

    @Test
    fun `Lesing av melding fra rapid skal produsere melding på nytt topic`() {
        val testrapid = TestRapid()
        val producer = MockProducer(true, null, StringSerializer(), StringSerializer())
        KandidatfeedLytter(testrapid, producer)

        testrapid.sendTestMessage(rapidMelding())

        Thread.sleep(500)

        assertThat(producer.history().size).isEqualTo(1)
        val melding = producer.history()[0]

        assertThat(melding.key()).isEqualTo("123")

        val resultatJson = jacksonObjectMapper().readTree(melding.value())
        val forventetJson = jacksonObjectMapper().readTree(rapidMelding())

        assertThat(resultatJson.get("cv")).isEqualTo(forventetJson.get("cv"))
        assertThat(resultatJson.get("veileder")).isEqualTo(forventetJson.get("veileder"))
        assertThat(resultatJson.get("aktørId")).isEqualTo(forventetJson.get("aktørId"))

        assertThat(resultatJson.has("system_read_count")).isFalse
        assertThat(resultatJson.has("system_participating_services")).isFalse
        assertThat(resultatJson.has("@event_name")).isFalse
    }

    private val cv = """
        {
            "meldingstype": "SLETT",
            "oppfolgingsinformasjon": null,
            "opprettCv": null,
            "endreCv": null,
            "slettCv": null,
            "opprettJobbprofil": null,
            "endreJobbprofil": null,
            "slettJobbprofil": null,
            "aktoerId": "123",
            "sistEndret": 1637238150.172
        }
    """.trimIndent()

    private val veileder = """
        {
             "aktorId":"123",
             "veilederId":"A123123",
             "tilordnet":"2021-11-19T13:18:03.307756228"
        }
    """.trimIndent()

    fun rapidMelding(): String = """
        {
          "aktørId": "123",
          "cv": $cv,
          "@event_name": "cv.sammenstilt",
          "system_read_count": 1,
          "system_participating_services": [
            {
              "service": "toi-cv",
              "instance": "toi-cv-58849d5f86-7qffs",
              "time": "2021-11-19T10:53:59.163725026"
            },
            {
              "service": "toi-sammenstille-kandidat",
              "instance": "toi-sammenstille-kandidat-85b9d49b9c-fctpx",
              "time": "2021-11-19T13:18:03.307756227"
            }
          ],
          "veileder":  $veileder
        }
    """.trimIndent()
}