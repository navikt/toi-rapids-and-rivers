package rapidpopulator

import no.nav.arbeidsgiver.toi.arenafritattkandidatsok.Fritatt
import no.nav.arbeidsgiver.toi.arenafritattkandidatsok.Status
import no.nav.arbeidsgiver.toi.arenafritattkandidatsok.kandidatlisteRepositoryMedLokalPostgres
import no.nav.arbeidsgiver.toi.arenafritattkandidatsok.slettAllDataIDatabase
import no.nav.arbeidsgiver.toi.rapidpopulator.FritattJobb
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate
import java.time.ZonedDateTime

private const val FRITATT_KANDIDATSØK_KEY = "fritattKandidatsøk"

class FritattJobbTest {
    private val repository = kandidatlisteRepositoryMedLokalPostgres()

    private val testRapid = TestRapid()

    private val fritattJobb = FritattJobb(repository, testRapid)

    @AfterEach
    fun cleanUp() {
        slettAllDataIDatabase()
    }

    @Test
    fun `skal dytte false-melding på rapid om det finnes en ny melding med periode som ikke har startet`() {
        repository.upsertFritatt(lagFritatt(periode = IkkeStartet))
        fritattJobb.run()
        val inspektør = testRapid.inspektør
        assertThat(inspektør.size).isEqualTo(1)
        assertThat(inspektør.message(0).get(FRITATT_KANDIDATSØK_KEY).booleanValue()).isFalse()
    }

    @Test
    fun `skal dytte true-melding på rapid om det finnes en ny melding med tidsbegrenset periode som har startet`() {
        repository.upsertFritatt(lagFritatt(periode = AktivTidsbegrenset))
        fritattJobb.run()
        val inspektør = testRapid.inspektør
        assertThat(inspektør.size).isEqualTo(1)
        assertThat(inspektør.message(0).get(FRITATT_KANDIDATSØK_KEY).booleanValue()).isTrue()
    }

    @Test
    fun `skal dytte true-melding på rapid om det finnes en ny melding med periode uten sluttdato som har startet`() {
        repository.upsertFritatt(lagFritatt(periode = AktivUtenSluttDato))
        fritattJobb.run()
        val inspektør = testRapid.inspektør
        assertThat(inspektør.size).isEqualTo(1)
        assertThat(inspektør.message(0).get(FRITATT_KANDIDATSØK_KEY).booleanValue()).isTrue()
    }

    @Test
    fun `skal dytte false-melding på rapid om det finnes en ny melding med periode som har sluttet`() {
        repository.upsertFritatt(lagFritatt(periode = Avsluttet))
        fritattJobb.run()
        val inspektør = testRapid.inspektør
        assertThat(inspektør.size).isEqualTo(1)
        assertThat(inspektør.message(0).get(FRITATT_KANDIDATSØK_KEY).booleanValue()).isFalse()
    }

    @Test
    fun `skal dytte false-melding på rapid om det finnes en ny melding med slettet satt til true`() {
        repository.upsertFritatt(lagFritatt(periode = AktivTidsbegrenset, slettetIArena = true))
        fritattJobb.run()
        val inspektør = testRapid.inspektør
        assertThat(inspektør.size).isEqualTo(1)
        assertThat(inspektør.message(0).get(FRITATT_KANDIDATSØK_KEY).booleanValue()).isFalse()
    }

    @Test
    fun `skal dytte true-melding på rapid om det finnes en melding som har tidligere rapportert at den ikke har startet men har startet nå`() {
        val fritatt = lagFritatt(periode = AktivTidsbegrenset)
        repository.upsertFritatt(fritatt)
        repository.markerSomSendt(fritatt, Status.FOER_FRITATT_PERIODE)
        fritattJobb.run()
        val inspektør = testRapid.inspektør
        assertThat(inspektør.size).isEqualTo(1)
        assertThat(inspektør.message(0).get(FRITATT_KANDIDATSØK_KEY).booleanValue()).isTrue()
    }

    @Test
    fun `skal dytte true-melding på rapid om det finnes en ny melding med periode som har startet der det allerede eksisterte en melding som tidligere hadde rapportert å ha sluttet`() {
        val fritatt = lagFritatt(periode = AktivTidsbegrenset)
        repository.markerSomSendt(fritatt, Status.FOER_FRITATT_PERIODE)
        repository.markerSomSendt(fritatt, Status.I_FRITATT_PERIODE)
        repository.markerSomSendt(fritatt, Status.ETTER_FRITATT_PERIODE)
        repository.upsertFritatt(fritatt)
        fritattJobb.run()
        val inspektør = testRapid.inspektør
        assertThat(inspektør.size).isEqualTo(1)
        assertThat(inspektør.message(0).get(FRITATT_KANDIDATSØK_KEY).booleanValue()).isTrue()
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
        private fun testFunksjoner() = FritattJobbTest().run { listOf(
                Arguments.of(::`skal dytte false-melding på rapid om det finnes en ny melding med periode som ikke har startet`),
                Arguments.of(::`skal dytte true-melding på rapid om det finnes en ny melding med tidsbegrenset periode som har startet`),
                Arguments.of(::`skal dytte true-melding på rapid om det finnes en ny melding med periode uten sluttdato som har startet`),
                Arguments.of(::`skal dytte false-melding på rapid om det finnes en ny melding med periode som har sluttet`),
                Arguments.of(::`skal dytte false-melding på rapid om det finnes en ny melding med slettet satt til true`),
                Arguments.of(::`skal dytte true-melding på rapid om det finnes en melding som har tidligere rapportert at den ikke har startet men har startet nå`),
                Arguments.of(::`skal dytte true-melding på rapid om det finnes en ny melding med periode som har startet der det allerede eksisterte en melding som tidligere hadde rapportert å ha sluttet`)
            ).stream()
        }
        private fun lagFritatt(periode: Periode, slettetIArena: Boolean = false) = Fritatt.ny(
            "12345678910", periode.startDato(), periode.sluttDato(),
            ZonedDateTime.now(), slettetIArena, "{}"
        )
        private sealed interface Periode {
            fun startDato(): LocalDate
            fun sluttDato(): LocalDate?
        }

        private object IkkeStartet: Periode {
            override fun startDato() = LocalDate.now().plusDays(10)
            override fun sluttDato() = null
        }

        private object Avsluttet: Periode {
            override fun startDato() = LocalDate.now().minusDays(10)
            override fun sluttDato() = LocalDate.now().minusDays(5)
        }

        private object AktivTidsbegrenset: Periode {
            override fun startDato() = LocalDate.now().minusDays(5)
            override fun sluttDato() = LocalDate.now().plusDays(5)
        }

        private object AktivUtenSluttDato: Periode {
            override fun startDato() = LocalDate.now().minusDays(5)
            override fun sluttDato() = null
        }
    }
}
