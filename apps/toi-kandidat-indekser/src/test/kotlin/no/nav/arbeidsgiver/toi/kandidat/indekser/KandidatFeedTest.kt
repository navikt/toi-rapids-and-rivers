package no.nav.arbeidsgiver.toi.kandidat.indekser

import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsCv
import no.nav.toi.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tools.jackson.databind.node.ObjectNode

/**
 * Tester scenarier hvor kandidat IKKE skal opprettes/lagres i ES.
 * Vi verifiserer eksplisitt at ESClient.lagreEsCv() ikke blir kalt.
 */
class KandidatFeedTest {

    private fun nyRapidMedLyttere(esClient: ESClient): TestRapid {
        val rapid = TestRapid()
        val pamUrl = "http://localhost:7664"
        SynligKandidatfeedLytter(rapid, esClient)
        UsynligKandidatfeedLytter(rapid, esClient)
        return rapid
    }

    @Test
    fun `Melding uten synlighet skal ikke opprette kandidat i ES`() {
        val esClient = mockk<ESClient>(relaxed = true)
        val rapid = nyRapidMedLyttere(esClient)
        val meldingMedKunCvOgAktørId = rapidMelding(synlighetJson = null)

        rapid.sendTestMessage(meldingMedKunCvOgAktørId)

        verify(exactly = 0) { esClient.lagreEsCv(any()) }
        verify(exactly = 0) { esClient.slettCv(any()) }
        confirmVerified(esClient)
    }

    @Test
    fun `Melding med kun CV og aktørId vil ikke opprette kandidat i ES`() {
        val esClient = mockk<ESClient>(relaxed = true)
        val rapid = nyRapidMedLyttere(esClient)
        val melding = rapidMelding(
            synlighetJson = synlighet(erSynlig = false, ferdigBeregnet = false),
            hullICv = null,
            ontologi = null
        )

        rapid.sendTestMessage(melding)

        verify(exactly = 0) { esClient.lagreEsCv(any()) }
        verify(exactly = 0) { esClient.slettCv(any()) }
        confirmVerified(esClient)
    }

    @Test
    fun `Synlighet ferdig beregnet men mangler dekte behov skal ikke lage kandidat`() {
        val esClient = mockk<ESClient>(relaxed = true)
        val rapid = nyRapidMedLyttere(esClient)
        val meldingSynlig = rapidMelding(synlighet(erSynlig = true, ferdigBeregnet = true))

        rapid.sendTestMessage(meldingSynlig)

        verify(exactly = 0) { esClient.lagreEsCv(any()) }
        verify(exactly = 0) { esClient.slettCv(any()) }
        confirmVerified(esClient)
    }

    @Test
    fun `Synlighet ferdig beregnet og dekte behov men slutt_av_hendelseskjede true skal ikke lage kandidat`() {
        val esClient = mockk<ESClient>(relaxed = true)
        val rapid = nyRapidMedLyttere(esClient)
        val melding = rapidMelding(
            synlighet(erSynlig = true, ferdigBeregnet = true),
            ontologi = ontologiDel(),
            sluttAvHendelseskjede = true
        )

        rapid.sendTestMessage(melding)

        verify(exactly = 0) { esClient.lagreEsCv(any()) }
        verify(exactly = 0) { esClient.slettCv(any()) }
        confirmVerified(esClient)
        assertThat(rapid.inspektør.size).isEqualTo(0)
    }

    @Test
    fun `Synlighet ikke ferdig beregnet skal ikke lage kandidat`() {
        val esClient = mockk<ESClient>(relaxed = true)
        val rapid = nyRapidMedLyttere(esClient)
        val meldingSynlig = rapidMelding(synlighet(erSynlig = true, ferdigBeregnet = false))

        rapid.sendTestMessage(meldingSynlig)

        verify(exactly = 0) { esClient.lagreEsCv(any()) }
        verify(exactly = 0) { esClient.slettCv(any()) }
        confirmVerified(esClient)
    }

    @Test
    fun `Usynlig melding med slutt_av_hendelseskjede true skal ikke trigge slett eller lagre`() {
        val esClient = mockk<ESClient>(relaxed = true)
        val rapid = nyRapidMedLyttere(esClient)
        val meldingUsynlig = rapidMelding(
            synlighet(erSynlig = false, ferdigBeregnet = true),
            sluttAvHendelseskjede = true,
            cvEksisterer = false
        )

        rapid.sendTestMessage(meldingUsynlig)

        verify(exactly = 0) { esClient.lagreEsCv(any()) }
        verify(exactly = 0) { esClient.slettCv(any()) }
        confirmVerified(esClient)
        assertThat(rapid.inspektør.size).isEqualTo(0)
    }

    @Test
    fun `Synlig melding med slutt_av_hendelseskjede true skal ikke trigge lagre`() {
        val esClient = mockk<ESClient>(relaxed = true)
        val rapid = nyRapidMedLyttere(esClient)
        val rapidMeldingMedSlutt = rapidMelding(
            synlighet(erSynlig = true, ferdigBeregnet = true),
            ontologi = ontologiDel(),
            sluttAvHendelseskjede = true
        )

        rapid.sendTestMessage(rapidMeldingMedSlutt)

        verify(exactly = 0) { esClient.lagreEsCv(any()) }
        verify(exactly = 0) { esClient.slettCv(any()) }
        confirmVerified(esClient)
        assertThat(rapid.inspektør.size).isEqualTo(0)
    }

    @Test
    fun `Meldinger der synlighet er ferdig beregnet og har dekte behov skal kandidat legges til i ES`() {
        val esClient = mockk<ESClient>(relaxed = true)
        val rapid = nyRapidMedLyttere(esClient)

        val melding = rapidMelding(
            synlighetJson = synlighet(erSynlig = true, ferdigBeregnet = true),
            ontologi = ontologiDel(),
            geografi = geografiDel()
        )

        rapid.sendTestMessage(melding)

        verify(exactly = 1) { esClient.lagreEsCv(any()) }
        verify(exactly = 0) { esClient.slettCv(any()) }
        confirmVerified(esClient)
        assertThat(rapid.inspektør.size).isEqualTo(1)
    }

    @Test
    fun `Melding der fødselsnummer i CV avviker fra roten indekseres med fødselsnummer fra roten`() {
        val esClient = mockk<ESClient>(relaxed = true)
        val rapid = nyRapidMedLyttere(esClient)
        val fødselsnummerFraIdentmapper = "0-fnr-fra-identmapper"
        val melding = rapidMelding(
            synlighetJson = synlighet(erSynlig = true, ferdigBeregnet = true),
            ontologi = ontologiDel(),
            geografi = geografiDel(),
            fødselsnummer = "0-annet-fnr-i-cv",
            fødselsnummerFraIdentmapper = fødselsnummerFraIdentmapper
        )

        rapid.sendTestMessage(melding)

        val lagretCv = slot<EsCv>()
        verify(exactly = 1) { esClient.lagreEsCv(capture(lagretCv)) }
        val mapper = esObjectMapper()
        val cvJson = mapper.readTree(mapper.writeValueAsString(lagretCv.captured))
        assertThat(cvJson["fodselsnummer"].asString()).isEqualTo(fødselsnummerFraIdentmapper)
        assertThat(rapid.inspektør.size).isEqualTo(1)
    }

    @Test
    fun `Melding med fødselsnummer null på roten indekseres med fødselsnummer fra CV`() {
        val esClient = mockk<ESClient>(relaxed = true)
        val rapid = nyRapidMedLyttere(esClient)
        val melding = rapidMelding(
            synlighetJson = synlighet(erSynlig = true, ferdigBeregnet = true),
            ontologi = ontologiDel(),
            geografi = geografiDel(),
            fødselsnummer = "0-fnr-fra-cv",
            fødselsnummerFraIdentmapper = null
        )

        rapid.sendTestMessage(melding)

        val lagretCv = slot<EsCv>()
        verify(exactly = 1) { esClient.lagreEsCv(capture(lagretCv)) }
        val mapper = esObjectMapper()
        val cvJson = mapper.readTree(mapper.writeValueAsString(lagretCv.captured))
        assertThat(cvJson["fodselsnummer"].asString()).isEqualTo("0-fnr-fra-cv")
    }

    @Test
    fun `Melding uten fødselsnummer på roten indekseres med fødselsnummer fra CV`() {
        val esClient = mockk<ESClient>(relaxed = true)
        val rapid = nyRapidMedLyttere(esClient)
        val mapper = esObjectMapper()
        val melding = mapper.readTree(
            rapidMelding(
                synlighetJson = synlighet(erSynlig = true, ferdigBeregnet = true),
                ontologi = ontologiDel(),
                geografi = geografiDel(),
                fødselsnummer = "0-fnr-fra-cv"
            )
        ) as ObjectNode
        melding.remove("fodselsnummer")

        rapid.sendTestMessage(melding.toString())

        val lagretCv = slot<EsCv>()
        verify(exactly = 1) { esClient.lagreEsCv(capture(lagretCv)) }
        val cvJson = mapper.readTree(mapper.writeValueAsString(lagretCv.captured))
        assertThat(cvJson["fodselsnummer"].asString()).isEqualTo("0-fnr-fra-cv")
    }

    @Test
    fun `Meldinger der synlighet er false skal kandidat slettes fra ES`() {
        val esClient = mockk<ESClient>(relaxed = true)
        val rapid = nyRapidMedLyttere(esClient)

        val melding = rapidMelding(
            synlighetJson = synlighet(erSynlig = false, ferdigBeregnet = true),
            cvEksisterer = false
        )

        rapid.sendTestMessage(melding)

        verify(exactly = 0) { esClient.lagreEsCv(any()) }
        verify(exactly = 1) { esClient.slettCv(any()) }
        confirmVerified(esClient)
        assertThat(rapid.inspektør.size).isEqualTo(1)
    }

    @Test
    fun `Melding som mangler jobbprofil skal godkjennes og lagres i ES`() {
        val esClient = mockk<ESClient>(relaxed = true)
        val rapid = nyRapidMedLyttere(esClient)

        val melding = rapidMelding(
            synlighetJson = synlighet(erSynlig = true, ferdigBeregnet = true),
            ontologi = ontologiDel(),
            geografi = geografiDel(),
            jobbprofilEksisterer = false
        )

        rapid.sendTestMessage(melding)

        verify(exactly = 1) { esClient.lagreEsCv(any()) }
        verify(exactly = 0) { esClient.slettCv(any()) }
        confirmVerified(esClient)
        assertThat(rapid.inspektør.size).isEqualTo(1)
    }
}