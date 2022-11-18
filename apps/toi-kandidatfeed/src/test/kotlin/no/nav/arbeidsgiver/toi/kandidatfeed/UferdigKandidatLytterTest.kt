package no.nav.arbeidsgiver.toi.kandidatfeed

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UferdigKandidatLytterTest {
    private val behovsListe = listOf("organisasjonsenhetsnavn", "hullICv", "ontologi")

    @Test
    fun `Melding uten behov-felt skal republiseres med behov`() {
        val meldingMedKunCvOgAktørId = rapidMelding(synlighetJson = synlighet(true))

        val testrapid = TestRapid()

        UferdigKandidatLytter(testrapid)
        testrapid.sendTestMessage(meldingMedKunCvOgAktørId)

        val inspektør = testrapid.inspektør
        assertThat(inspektør.size).isEqualTo(1)
        val melding = inspektør.message(0)
        assertThat(melding["@behov"].asIterable()).map<String>(JsonNode::asText).containsAll(behovsListe)
    }

    @Test
    fun `Melding skal legge ved ontologi kompetanse og stillingstitler den ønsker ontologi på`() {
        val meldingMedKunCvOgAktørId = rapidMelding(synlighetJson = synlighet(true))

        val testrapid = TestRapid()

        UferdigKandidatLytter(testrapid)
        testrapid.sendTestMessage(meldingMedKunCvOgAktørId)

        val inspektør = testrapid.inspektør
        assertThat(inspektør.size).isEqualTo(1)
        val melding = inspektør.message(0)
        assertThat(melding["stillingstittel"].asIterable()).map<String>(JsonNode::asText)
            .containsExactlyInAnyOrder("Lege", "Pianolærer", "Baker", "Sjåfør", "Sirkustekniker", "Produktsjef kjøretøy")
        assertThat(melding["kompetanse"].asIterable()).map<String>(JsonNode::asText)
            .containsExactlyInAnyOrder("Sirkusestetikk", "Sirkusvokabular", "Definere riggebehov for sirkuskunster", "Kontrollere sirkusrigging før fremføring", "Servicearbeid")
    }

    @Test
    fun `Melding uten gitte behov-verdier skal republiseres med behov`() {
        val meldingMedKunCvOgAktørId = rapidMelding(synlighetJson = synlighet(true),
            behovsListe = listOf("uinteressant_behov"))

        val testrapid = TestRapid()
        UferdigKandidatLytter(testrapid)
        testrapid.sendTestMessage(meldingMedKunCvOgAktørId)

        val inspektør = testrapid.inspektør
        assertThat(inspektør.size).isEqualTo(1)
        val melding = inspektør.message(0)
        assertThat(melding["@behov"].asIterable()).map<String>(JsonNode::asText).containsAll(behovsListe)
    }

    @Test
    fun `Melding med gitte behov-verdier skal ignoreres`() {
        val meldingMedKunCvOgAktørId = rapidMelding(synlighetJson = synlighet(true),
            behovsListe = behovsListe)

        val testrapid = TestRapid()

        UferdigKandidatLytter(testrapid)
        testrapid.sendTestMessage(meldingMedKunCvOgAktørId)

        val inspektør = testrapid.inspektør
        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `Melding uten synlighet ignoreres`() {
        val meldingMedKunCvOgAktørId = rapidMelding(synlighetJson = null)

        val testrapid = TestRapid()

        UferdigKandidatLytter(testrapid)
        testrapid.sendTestMessage(meldingMedKunCvOgAktørId)

        val inspektør = testrapid.inspektør
        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `Melding med synlighet false har ikke informasjonsbehov`() {
        val meldingMedKunCvOgAktørId = rapidMelding(synlighetJson = synlighet(false))

        val testrapid = TestRapid()

        UferdigKandidatLytter(testrapid)
        testrapid.sendTestMessage(meldingMedKunCvOgAktørId)

        val inspektør = testrapid.inspektør
        assertThat(inspektør.size).isEqualTo(0)
    }
}