package no.nav.arbeidsgiver.toi.kandidatfeed

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.apache.kafka.clients.producer.MockProducer
import org.apache.kafka.common.serialization.StringSerializer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KandidatfeedTest {
    @Test
    fun `Melding med kun CV og aktørId ikke produsere melding på kandidat-topic`() {
        val meldingMedKunCvOgAktørId = rapidMelding(synlighetJson = "")

        val testrapid = TestRapid()
        val producer = MockProducer(true, null, StringSerializer(), StringSerializer())

        SynligKandidatfeedLytter(testrapid, producer)
        UsynligKandidatfeedLytter(testrapid, producer)
        testrapid.sendTestMessage(meldingMedKunCvOgAktørId)

        assertThat(producer.history().size).isEqualTo(0)
    }

    @Test
    fun `Meldinger der synlighet er ferdig beregnet til true men dekte behov ikke eksisterer på meldingen skal melding ikke legges på kandidat-topic`() {
        val meldingSynlig = rapidMelding(synlighet(erSynlig = true, ferdigBeregnet = true))

        val testrapid = TestRapid()
        val producer = MockProducer(true, null, StringSerializer(), StringSerializer())

        SynligKandidatfeedLytter(testrapid, producer)
        UsynligKandidatfeedLytter(testrapid, producer)

        testrapid.sendTestMessage(meldingSynlig)

        assertThat(producer.history().size).isEqualTo(0)
    }

    @Test
    fun `Meldinger der synlighet er ferdig beregnet til false men dekte behov ikke eksisterer skal melding likevel legges på kandidat-topic`() {
        val meldingUsynlig = rapidMelding(synlighet(erSynlig = false, ferdigBeregnet = true))

        val testrapid = TestRapid()
        val producer = MockProducer(true, null, StringSerializer(), StringSerializer())

        SynligKandidatfeedLytter(testrapid, producer)
        UsynligKandidatfeedLytter(testrapid, producer)

        testrapid.sendTestMessage(meldingUsynlig)

        assertThat(producer.history().size).isEqualTo(1)
    }

    @Test
    fun `Meldinger der synlighet er ferdig beregnet og har dekte behov skal produsere melding på kandidat-topic`() {
        val tomJson = """{}"""
        val meldingSynlig = rapidMelding(
            synlighet(erSynlig = true, ferdigBeregnet = true),
            organisasjonsenhetsnavn = "NAV et kontor",
            hullICv = tomJson,
            ontologi = tomJson
        )
        val meldingUsynlig = rapidMelding(
            synlighet(erSynlig = false, ferdigBeregnet = true),
            organisasjonsenhetsnavn = "NAV et kontor",
            hullICv = tomJson,
            ontologi = tomJson
        )

        val testrapid = TestRapid()
        val producer = MockProducer(true, null, StringSerializer(), StringSerializer())

        SynligKandidatfeedLytter(testrapid, producer)
        UsynligKandidatfeedLytter(testrapid, producer)

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

        SynligKandidatfeedLytter(testrapid, producer)
        UsynligKandidatfeedLytter(testrapid, producer)
        testrapid.sendTestMessage(meldingSynlig)

        assertThat(producer.history().size).isEqualTo(0)
    }

    @Test
    fun `Informasjon om kandidaten skal sendes videre til kandidat-topic`() {
        val rapidMelding =
            rapidMelding(synlighet(erSynlig = true, ferdigBeregnet = true), organisasjonsenhetsnavn = "NAV et kontor", hullICv = "{}", ontologi = "{}")
        val testrapid = TestRapid()
        val producer = MockProducer(true, null, StringSerializer(), StringSerializer())

        SynligKandidatfeedLytter(testrapid, producer)
        UsynligKandidatfeedLytter(testrapid, producer)

        testrapid.sendTestMessage(rapidMelding)

        assertThat(producer.history().size).isEqualTo(1)
        val melding = producer.history()[0]

        assertThat(melding.key()).isEqualTo("123")

        val resultatJson = jacksonObjectMapper().readTree(melding.value())
        val forventetJson = jacksonObjectMapper().readTree(rapidMelding)

        assertThat(resultatJson.get("arbeidsmarkedCv")).isNotNull.isEqualTo(forventetJson.get("arbeidsmarkedCv"))
        assertThat(resultatJson.get("veileder")).isNotNull.isEqualTo(forventetJson.get("veileder"))
        assertThat(resultatJson.get("aktørId")).isNotNull.isEqualTo(forventetJson.get("aktørId"))

        assertThat(resultatJson.has("system_read_count")).isFalse
        assertThat(resultatJson.has("system_participating_services")).isFalse
        assertThat(resultatJson.has("@event_name")).isFalse

        assertThat(resultatJson.get("oppfølgingsinformasjon").get("oppfolgingsenhet").asText()).isEqualTo("1234")
        assertThat(resultatJson.get("organisasjonsenhetsnavn").asText()).isEqualTo("NAV et kontor")
        assertThat(resultatJson.get("hullICv").isMissingOrNull()).isFalse
        assertThat(resultatJson.get("ontologi").isMissingOrNull()).isFalse
    }

    @Test
    fun `UsynligKandidatfeedLytter leser ikke melding om slutt_av_hendelseskjede er true`() {
        val meldingUsynlig = rapidMelding(synlighet(erSynlig = false, ferdigBeregnet = true), sluttAvHendelseskjede = true)

        val testrapid = TestRapid()
        val producer = MockProducer(true, null, StringSerializer(), StringSerializer())

        SynligKandidatfeedLytter(testrapid, producer)
        UsynligKandidatfeedLytter(testrapid, producer)

        testrapid.sendTestMessage(meldingUsynlig)

        assertThat(producer.history().size).isEqualTo(0)
        assertThat(testrapid.inspektør.size).isEqualTo(0)
    }

    @Test
    fun `SynligKandidatfeedLytter leser ikke melding om slutt_av_hendelseskjede er true`() {
        val rapidMelding =
            rapidMelding(synlighet(erSynlig = true, ferdigBeregnet = true), organisasjonsenhetsnavn = "NAV et kontor", hullICv = "{}", ontologi = "{}", sluttAvHendelseskjede = true)
        val testrapid = TestRapid()
        val producer = MockProducer(true, null, StringSerializer(), StringSerializer())

        SynligKandidatfeedLytter(testrapid, producer)
        UsynligKandidatfeedLytter(testrapid, producer)

        testrapid.sendTestMessage(rapidMelding)

        assertThat(producer.history().size).isEqualTo(0)
        assertThat(testrapid.inspektør.size).isEqualTo(0)
    }

    @Test
    fun `UsynligKandidatfeedLytter legger tilbake melding med slutt_av_hendelseskjede satt til true`() {
        val meldingUsynlig = rapidMelding(synlighet(erSynlig = false, ferdigBeregnet = true))

        val testrapid = TestRapid()
        val producer = MockProducer(true, null, StringSerializer(), StringSerializer())

        SynligKandidatfeedLytter(testrapid, producer)
        UsynligKandidatfeedLytter(testrapid, producer)

        testrapid.sendTestMessage(meldingUsynlig)

        assertThat(testrapid.inspektør.size).isEqualTo(1)
        assertThat(testrapid.inspektør.message(0).get("@slutt_av_hendelseskjede").booleanValue()).isEqualTo(true)
    }

    @Test
    fun `SynligKandidatfeedLytter legger tilbake melding med slutt_av_hendelseskjede satt til true`() {
        val rapidMelding =
            rapidMelding(synlighet(erSynlig = true, ferdigBeregnet = true), organisasjonsenhetsnavn = "NAV et kontor", hullICv = "{}", ontologi = "{}")

        val testrapid = TestRapid()
        val producer = MockProducer(true, null, StringSerializer(), StringSerializer())

        SynligKandidatfeedLytter(testrapid, producer)
        UsynligKandidatfeedLytter(testrapid, producer)

        testrapid.sendTestMessage(rapidMelding)

        assertThat(testrapid.inspektør.size).isEqualTo(1)
        assertThat(testrapid.inspektør.message(0).get("@slutt_av_hendelseskjede").booleanValue()).isEqualTo(true)
    }
}