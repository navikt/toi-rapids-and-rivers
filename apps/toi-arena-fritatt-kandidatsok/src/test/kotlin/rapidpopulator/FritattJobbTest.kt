package rapidpopulator

import no.nav.arbeidsgiver.toi.arenafritattkandidatsok.*
import no.nav.arbeidsgiver.toi.rapidpopulator.FritattJobb
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate
import java.time.ZonedDateTime

private const val FRITATT_KANDIDATSØK_KEY = "erFritatt"

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class FritattJobbTest {
    private val repository = kandidatlisteRepositoryMedLokalPostgres()

    private val fritattJobb = FritattJobb(repository, testRapid)

    @AfterEach
    fun cleanUp() {
        slettAllDataIDatabase()
        testRapid.reset()
    }

    @Test
    fun `skal dytte false-melding på rapid om det kommer en første melding med periode som ikke har startet`() {
        repository.upsertFritatt(lagFritatt(periode = IkkeStartetFørIMorgen))
        fritattJobb.run()
        val inspektør = testRapid.inspektør
        assertThat(inspektør.size).isEqualTo(1)
        assertThat(inspektør.message(0).get(FRITATT_KANDIDATSØK_KEY).booleanValue()).isFalse()
    }

    @Test
    fun `skal dytte true-melding på rapid om det kommer en første melding med tidsbegrenset periode som har startet`() {
        repository.upsertFritatt(lagFritatt(periode = AktivTidsbegrensetTilOgMedIDag))
        fritattJobb.run()
        val inspektør = testRapid.inspektør
        assertThat(inspektør.size).isEqualTo(1)
        assertThat(inspektør.message(0).get(FRITATT_KANDIDATSØK_KEY).booleanValue()).isTrue()
    }

    @Test
    fun `skal dytte true-melding på rapid om det kommer en første melding med periode uten sluttdato som har startet`() {
        repository.upsertFritatt(lagFritatt(periode = AktivUtenSluttDatoFraOgMedIDag))
        fritattJobb.run()
        val inspektør = testRapid.inspektør
        assertThat(inspektør.size).isEqualTo(1)
        assertThat(inspektør.message(0).get(FRITATT_KANDIDATSØK_KEY).booleanValue()).isTrue()
    }

    @Test
    fun `skal dytte false-melding på rapid om det kommer en første melding med periode som har sluttet`() {
        repository.upsertFritatt(lagFritatt(periode = AvsluttetIGår))
        fritattJobb.run()
        val inspektør = testRapid.inspektør
        assertThat(inspektør.size).isEqualTo(1)
        assertThat(inspektør.message(0).get(FRITATT_KANDIDATSØK_KEY).booleanValue()).isFalse()
    }

    @Test
    fun `skal dytte false-melding på rapid om det kommer en første melding med slettet satt til true, og vi er i en aktiv periode`() {
        repository.upsertFritatt(lagFritatt(periode = AktivTidsbegrensetTilOgMedIDag, slettetIArena = true))
        fritattJobb.run()
        val inspektør = testRapid.inspektør
        assertThat(inspektør.size).isEqualTo(1)
        assertThat(inspektør.message(0).get(FRITATT_KANDIDATSØK_KEY).booleanValue()).isFalse()
    }

    @Test
    fun `skal dytte true-melding på rapid om det finnes en melding som har tidligere rapportert at den ikke har startet men har startet nå`() {
        val fritatt = lagFritatt(periode = AktivTidsbegrensetTilOgMedIDag)
        repository.upsertFritatt(fritatt)
        repository.markerSomSendt(fritatt, Status.FOER_FRITATT_PERIODE)
        fritattJobb.run()
        val inspektør = testRapid.inspektør
        assertThat(inspektør.size).isEqualTo(1)
        assertThat(inspektør.message(0).get(FRITATT_KANDIDATSØK_KEY).booleanValue()).isTrue()
    }

    @Test
    fun `skal dytte false-melding på rapid om det finnes en melding som har tidligere rapportert at den har startet men har stanset nå`() {
        val fritatt = lagFritatt(periode = AvsluttetIGår)
        repository.upsertFritatt(fritatt)
        repository.markerSomSendt(fritatt, Status.I_FRITATT_PERIODE)
        fritattJobb.run()
        val inspektør = testRapid.inspektør
        assertThat(inspektør.size).isEqualTo(1)
        assertThat(inspektør.message(0).get(FRITATT_KANDIDATSØK_KEY).booleanValue()).isFalse()
    }

    @Test
    fun `skal dytte true-melding på rapid om det finnes en ny melding med periode som har startet der det allerede eksisterte en melding som tidligere hadde rapportert å ha sluttet`() {


        val fritattGammel = lagFritatt(periode = AvsluttetIGår)
        repository.markerSomSendt(fritattGammel, Status.FOER_FRITATT_PERIODE)
        repository.markerSomSendt(fritattGammel, Status.I_FRITATT_PERIODE)
        repository.markerSomSendt(fritattGammel, Status.ETTER_FRITATT_PERIODE)
        repository.upsertFritatt(fritattGammel)

        repository.upsertFritatt(lagFritatt(periode = AktivTidsbegrensetTilOgMedIDag))

        fritattJobb.run()
        val inspektør = testRapid.inspektør
        assertThat(inspektør.size).isEqualTo(1)
        assertThat(inspektør.message(0).get(FRITATT_KANDIDATSØK_KEY).booleanValue()).isTrue()
    }

    @Test
    fun `skal ikke sende melding dersom fritatt melding har blitt endret mens scheduleren kjører`() {
        val fritattFraSchedulerspørring =
            lagFritatt(periode = IkkeStartetFørIMorgen, sistEndretIArena = ZonedDateTime.now().minusDays(1))

        val fritattSomKommmerIMellom = lagFritatt(
            periode = AktivUtenSluttDatoFraOgMedIDag,
            sistEndretIArena = ZonedDateTime.now()
        )
        repository.upsertFritatt(
            fritattFraSchedulerspørring
        )
        repository.upsertFritatt(
            fritattSomKommmerIMellom
        )
        repository.markerSomSendt(fritatt = fritattFraSchedulerspørring, Status.FOER_FRITATT_PERIODE)

        val inspektør = testRapid.inspektør
        assertThat(inspektør.size).isEqualTo(1)
        assertThat(inspektør.message(0).get(FRITATT_KANDIDATSØK_KEY).booleanValue()).isFalse()

        val statuser = hentAlleStatusene()
        assertThat(statuser).hasSize(0)

        fritattJobb.run()
        val inspektørNesteNatt = testRapid.inspektør
        assertThat(inspektørNesteNatt.size).isEqualTo(2)
        assertThat(inspektørNesteNatt.message(1).get(FRITATT_KANDIDATSØK_KEY).booleanValue()).isTrue()

        val statuserNesteNatt = hentAlleStatusene()
        assertThat(statuserNesteNatt).hasSize(1)
        assertThat(statuserNesteNatt.first().second).isEqualTo(Status.I_FRITATT_PERIODE)
    }

    @ParameterizedTest
    @MethodSource("testFunksjoner")
    fun `skal ikke sende melding på nytt neste kjøring om den allerede er blitt kjørt`(testCase: () -> Unit) {
        testCase()
        assertThat(testRapid.inspektør.size).isEqualTo(1)
        fritattJobb.run()
        assertThat(testRapid.inspektør.size).isEqualTo(1)
    }

    companion object {
        @JvmStatic
        private fun testFunksjoner() = FritattJobbTest().run {
            listOf(
                Arguments.of(::`skal dytte true-melding på rapid om det kommer en første melding med tidsbegrenset periode som har startet`),
                Arguments.of(::`skal dytte false-melding på rapid om det kommer en første melding med periode som ikke har startet`),
                Arguments.of(::`skal dytte true-melding på rapid om det kommer en første melding med periode uten sluttdato som har startet`),
                Arguments.of(::`skal dytte false-melding på rapid om det kommer en første melding med periode som har sluttet`),
                Arguments.of(::`skal dytte false-melding på rapid om det kommer en første melding med slettet satt til true, og vi er i en aktiv periode`),
                Arguments.of(::`skal dytte true-melding på rapid om det finnes en melding som har tidligere rapportert at den ikke har startet men har startet nå`),
                Arguments.of(::`skal dytte true-melding på rapid om det finnes en ny melding med periode som har startet der det allerede eksisterte en melding som tidligere hadde rapportert å ha sluttet`)
            ).stream()
        }

        private fun lagFritatt(
            periode: Periode,
            slettetIArena: Boolean = false,
            sistEndretIArena: ZonedDateTime = ZonedDateTime.now(),
        ) = Fritatt.ny(
            "12345678910", periode.startDato(), periode.sluttDato(),
            sistEndretIArena, slettetIArena, "{}"
        )

        private sealed interface Periode {
            fun startDato(): LocalDate
            fun sluttDato(): LocalDate?
        }

        private object IkkeStartetFørIMorgen : Periode {
            override fun startDato() = LocalDate.now().plusDays(1)
            override fun sluttDato() = null
        }

        private object AvsluttetIGår : Periode {
            override fun startDato() = LocalDate.now().minusDays(10)
            override fun sluttDato() = LocalDate.now().minusDays(1)
        }

        private object AktivTidsbegrensetTilOgMedIDag : Periode {
            override fun startDato() = LocalDate.now().minusDays(5)
            override fun sluttDato() = LocalDate.now()
        }

        private object AktivUtenSluttDatoFraOgMedIDag : Periode {
            override fun startDato() = LocalDate.now()
            override fun sluttDato() = null
        }

        private val testRapid = TestRapid()

    }
}
