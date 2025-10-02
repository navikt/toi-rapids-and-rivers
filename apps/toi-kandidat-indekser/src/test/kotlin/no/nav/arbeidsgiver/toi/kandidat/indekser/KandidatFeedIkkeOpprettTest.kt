package no.nav.arbeidsgiver.toi.kandidat.indekser

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.verify
import no.nav.toi.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Tester scenarier hvor kandidat IKKE skal opprettes/lagres i ES.
 * Vi verifiserer eksplisitt at ESClient.lagreEsCv() ikke blir kalt.
 */
class KandidatFeedIkkeOpprettTest {

    private fun nyRapidMedLyttere(esClient: ESClient): TestRapid {
        val rapid = TestRapid()
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
            ontologi = null,
            organisasjonsenhetsnavn = null
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
            organisasjonsenhetsnavn = "NAV et kontor",
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
            sluttAvHendelseskjede = true
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
            organisasjonsenhetsnavn = "NAV et kontor",
            hullICv = "{}",
            ontologi = "{}",
            sluttAvHendelseskjede = true
        )

        rapid.sendTestMessage(rapidMeldingMedSlutt)

        verify(exactly = 0) { esClient.lagreEsCv(any()) }
        verify(exactly = 0) { esClient.slettCv(any()) }
        confirmVerified(esClient)
        assertThat(rapid.inspektør.size).isEqualTo(0)
    }
}

