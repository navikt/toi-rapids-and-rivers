package no.nav.arbeidsgiver.toi.kandidat.indekser

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsCv
import no.nav.toi.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.slf4j.LoggerFactory

/**
 * Tester scenarier hvor kandidat IKKE skal opprettes/lagres i ES.
 * Vi verifiserer eksplisitt at ESClient.lagreEsCv() ikke blir kalt.
 */
class KandidatFeedTest {
    private val logger = LoggerFactory.getLogger(SynligKandidatfeedLytter::class.java) as Logger
    private lateinit var logghendelser: ListAppender<ILoggingEvent>

    @BeforeEach
    fun fangLogghendelser() {
        logghendelser = ListAppender<ILoggingEvent>().apply { start() }
        logger.addAppender(logghendelser)
    }

    @AfterEach
    fun stoppLoggfangst() {
        logger.detachAppender(logghendelser)
        logghendelser.stop()
    }

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
    fun `Melding med utdatert fødselsnummer i CV logger avvik og fortsetter med fødselsnummer fra roten`() {
        val esClient = mockk<ESClient>(relaxed = true)
        val rapid = nyRapidMedLyttere(esClient)
        val fødselsnummerFraIdentmapper = "0-fnr-fra-identmapper"
        val melding = rapidMelding(
            synlighetJson = synlighet(erSynlig = true, ferdigBeregnet = true),
            ontologi = ontologiDel(),
            geografi = geografiDel(),
            aktørId = "aktor-id-test",
            fødselsnummer = "0-utdatert-fnr-i-cv",
            fødselsnummerFraIdentmapper = fødselsnummerFraIdentmapper
        )

        rapid.sendTestMessage(melding)

        val lagretCv = slot<EsCv>()
        verify(exactly = 1) { esClient.lagreEsCv(capture(lagretCv)) }
        val mapper = esObjectMapper()
        val cvJson = mapper.readTree(mapper.writeValueAsString(lagretCv.captured))
        assertThat(cvJson["fodselsnummer"].asString()).isEqualTo(fødselsnummerFraIdentmapper)

        val advarsler = logghendelser.list.filter { it.level == Level.WARN }
        assertThat(advarsler).hasSize(2)
        assertThat(advarsler.map { it.markerList.orEmpty().map { marker -> marker.name } })
            .containsExactlyInAnyOrder(listOf("TEAM_LOGS"), emptyList<String>())
        advarsler.forEach {
            assertThat(it.formattedMessage)
                .contains("arbeidsmarkedCv")
                .doesNotContain("aktor-id-test", fødselsnummerFraIdentmapper, "0-utdatert-fnr-i-cv")
            assertThat(it.throwableProxy).isNull()
        }

        rapid.sendTestMessage(
            rapidMelding(
                synlighetJson = synlighet(erSynlig = true, ferdigBeregnet = true),
                ontologi = ontologiDel(),
                geografi = geografiDel(),
                fødselsnummer = fødselsnummerFraIdentmapper
            )
        )

        verify(exactly = 2) { esClient.lagreEsCv(any()) }
        assertThat(rapid.inspektør.size).isEqualTo(2)
        assertThat(logghendelser.list.filter { it.level == Level.WARN }).hasSize(2)
        assertThat(logghendelser.list.filter { it.level == Level.ERROR }).isEmpty()
    }

    @ParameterizedTest
    @CsvSource(
        "0-fnr-fra-roten, 0",
        "0-utdatert-fnr-i-oppfolging, 2",
        "NULL, 0",
        nullValues = ["NULL"]
    )
    fun `Fødselsnummer i oppfølging gir bare advarsel ved avvik`(
        fødselsnummerFraOppfølging: String?,
        forventetAntallAdvarsler: Int
    ) {
        val esClient = mockk<ESClient>(relaxed = true)
        val rapid = nyRapidMedLyttere(esClient)
        val fødselsnummerFraRoten = "0-fnr-fra-roten"
        val melding = rapidMelding(
            synlighetJson = synlighet(erSynlig = true, ferdigBeregnet = true),
            ontologi = ontologiDel(),
            geografi = geografiDel(),
            aktørId = "aktor-id-test",
            fødselsnummer = fødselsnummerFraRoten,
            fødselsnummerFraOppfølging = fødselsnummerFraOppfølging
        )

        rapid.sendTestMessage(melding)

        val lagretCv = slot<EsCv>()
        verify(exactly = 1) { esClient.lagreEsCv(capture(lagretCv)) }
        val mapper = esObjectMapper()
        val cvJson = mapper.readTree(mapper.writeValueAsString(lagretCv.captured))
        assertThat(cvJson["fodselsnummer"].asString()).isEqualTo(fødselsnummerFraRoten)
        assertThat(rapid.inspektør.size).isEqualTo(1)
        val advarsler = logghendelser.list.filter { it.level == Level.WARN }
        assertThat(advarsler).hasSize(forventetAntallAdvarsler)
        assertThat(logghendelser.list.filter { it.level == Level.ERROR }).isEmpty()
        advarsler.forEach {
            assertThat(it.formattedMessage)
                .contains("oppfølgingsinformasjon")
                .doesNotContain("arbeidsmarkedCv", "aktor-id-test", fødselsnummerFraRoten, "0-utdatert-fnr-i-oppfolging")
        }
    }

    @ParameterizedTest
    @CsvSource("true", "false")
    fun `Melding uten fødselsnummer på roten indekseres med fødselsnummer fra CV`(nullPåRoten: Boolean) {
        val esClient = mockk<ESClient>(relaxed = true)
        val rapid = nyRapidMedLyttere(esClient)
        val fødselsnummerFraCv = "0-fnr-fra-cv"
        val melding = rapidMelding(
            synlighetJson = synlighet(erSynlig = true, ferdigBeregnet = true),
            ontologi = ontologiDel(),
            geografi = geografiDel(),
            fødselsnummer = fødselsnummerFraCv,
            fødselsnummerFraIdentmapper = null
        ).let { if (nullPåRoten) it else it.replace(""""fodselsnummer": null,""", "") }

        rapid.sendTestMessage(melding)

        val lagretCv = slot<EsCv>()
        verify(exactly = 1) { esClient.lagreEsCv(capture(lagretCv)) }
        val mapper = esObjectMapper()
        val cvJson = mapper.readTree(mapper.writeValueAsString(lagretCv.captured))
        assertThat(cvJson["fodselsnummer"].asString()).isEqualTo(fødselsnummerFraCv)
        assertThat(rapid.inspektør.size).isEqualTo(1)
        assertThat(logghendelser.list.filter { it.level == Level.WARN || it.level == Level.ERROR }).isEmpty()
    }

    @Test
    fun `Meldinger der synlighet er false skal kandidat slettes fra ES`() {
        val esClient = mockk<ESClient>(relaxed = true)
        val rapid = nyRapidMedLyttere(esClient)

        val melding = rapidMelding(
            synlighetJson = synlighet(erSynlig = false, ferdigBeregnet = true),
            cvEksisterer = false,
            fødselsnummerFraIdentmapper = "0-fnr-fra-identmapper",
            fødselsnummerFraOppfølging = "0-utdatert-fnr-i-oppfolging"
        )

        rapid.sendTestMessage(melding)

        verify(exactly = 0) { esClient.lagreEsCv(any()) }
        verify(exactly = 1) { esClient.slettCv(any()) }
        confirmVerified(esClient)
        assertThat(rapid.inspektør.size).isEqualTo(1)
        assertThat(logghendelser.list.filter { it.level == Level.WARN || it.level == Level.ERROR }).isEmpty()
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