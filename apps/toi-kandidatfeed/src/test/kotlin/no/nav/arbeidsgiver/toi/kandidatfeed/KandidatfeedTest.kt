package no.nav.arbeidsgiver.toi.kandidatfeed

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.apache.kafka.clients.producer.MockProducer
import org.apache.kafka.common.serialization.StringSerializer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KandidatfeedTest {

    @Test
    fun `Hvis appen kjører i prod, skal melding med kun CV og aktørId produsere melding på kandidat-topic`() {
        val meldingMedKunCvOgAktørId = rapidMelding(synlighetJson = "")

        val testrapid = TestRapid()
        val producer = MockProducer(true, null, StringSerializer(), StringSerializer())

        KandidatfeedLytter(testrapid, producer, erProd = true)

        testrapid.sendTestMessage(meldingMedKunCvOgAktørId)

        assertThat(producer.history().size).isEqualTo(1)
        val melding = producer.history()[0]

        val meldingPåRapid = jacksonObjectMapper().readTree(meldingMedKunCvOgAktørId)
        val meldingPåKafka = jacksonObjectMapper().readTree(melding.value())

        assertThat(meldingPåRapid["aktørId"]).isEqualTo(meldingPåKafka["aktørId"])
        assertThat(meldingPåRapid["cv"]).isEqualTo(meldingPåKafka["cv"])
        assertThat(meldingPåRapid["veileder"]).isEqualTo(meldingPåKafka["veileder"])
    }

    @Test
    fun `Hvis appen kjører i dev, skal melding med kun CV og aktørId ikke produsere melding på kandidat-topic`() {
        val meldingMedKunCvOgAktørId = rapidMelding(synlighetJson = "")

        val testrapid = TestRapid()
        val producer = MockProducer(true, null, StringSerializer(), StringSerializer())

        KandidatfeedLytter(testrapid, producer, erProd = false)
        testrapid.sendTestMessage(meldingMedKunCvOgAktørId)

        assertThat(producer.history().size).isEqualTo(0)
    }

    @Test
    fun `Meldinger der synlighet er ferdig beregnet skal produsere melding på kandidat-topic`() {
        val meldingSynlig = rapidMelding(synlighet(erSynlig = true, ferdigBeregnet = true))
        val meldingUsynlig = rapidMelding(synlighet(erSynlig = false, ferdigBeregnet =  true))

        val testrapid = TestRapid()
        val producer = MockProducer(true, null, StringSerializer(), StringSerializer())

        KandidatfeedLytter(testrapid, producer, erProd = false)

        testrapid.sendTestMessage(meldingSynlig)
        testrapid.sendTestMessage(meldingUsynlig)

        assertThat(producer.history().size).isEqualTo(2)
        val melding = producer.history()[0]
        val melding2 = producer.history()[1]

        val json = jacksonObjectMapper().readTree(melding.value())["synlighet"]
        val json2 = jacksonObjectMapper().readTree(melding2.value())["synlighet"]

        assertThat(json["ferdigBeregnet"].asBoolean()).isTrue
        assertThat(json2["ferdigBeregnet"].asBoolean()).isTrue
        assertThat(json["erSynlig"].asBoolean()).isTrue
        assertThat(json2["erSynlig"].asBoolean()).isFalse
    }

    @Test
    fun `Meldinger der synlighet ikke er ferdig beregnet skal ikke produsere melding på kandidat-topic`() {
        val meldingSynlig = rapidMelding(synlighet(erSynlig = true, ferdigBeregnet = false))

        val testrapid = TestRapid()
        val producer = MockProducer(true, null, StringSerializer(), StringSerializer())

        KandidatfeedLytter(testrapid, producer, erProd = false)
        testrapid.sendTestMessage(meldingSynlig)

        assertThat(producer.history().size).isEqualTo(0)
    }

    @Test
    fun `Informasjon om kandidaten skal sendes videre til kandidat-topic`() {
        val rapidMelding = rapidMelding(synlighet(erSynlig = true, ferdigBeregnet = true))
        val testrapid = TestRapid()
        val producer = MockProducer(true, null, StringSerializer(), StringSerializer())

        KandidatfeedLytter(testrapid, producer, erProd = false)

        testrapid.sendTestMessage(rapidMelding)

        assertThat(producer.history().size).isEqualTo(1)
        val melding = producer.history()[0]

        assertThat(melding.key()).isEqualTo("123")

        val resultatJson = jacksonObjectMapper().readTree(melding.value())
        val forventetJson = jacksonObjectMapper().readTree(rapidMelding)

        assertThat(resultatJson.get("cv")).isEqualTo(forventetJson.get("cv"))
        assertThat(resultatJson.get("veileder")).isEqualTo(forventetJson.get("veileder"))
        assertThat(resultatJson.get("aktørId")).isEqualTo(forventetJson.get("aktørId"))

        assertThat(resultatJson.has("system_read_count")).isFalse
        assertThat(resultatJson.has("system_participating_services")).isFalse
        assertThat(resultatJson.has("@event_name")).isFalse
    }

    private fun synlighet(erSynlig: Boolean = true, ferdigBeregnet: Boolean = true) = """
        "synlighet": {
            "erSynlig": "$erSynlig",
            "ferdigBeregnet": "$ferdigBeregnet"
        },
    """.trimIndent()

    fun rapidMelding(synlighetJson: String?): String = """
        {
          "@event_name": "cv.sammenstilt",
          "aktørId": "123",
          "cv": {
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
          },
          "veileder": {
             "aktorId":"123",
             "veilederId":"A123123",
             "tilordnet":"2021-11-19T13:18:03.307756228"
          },
          $synlighetJson
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
          ]
        }
    """.trimIndent()
}