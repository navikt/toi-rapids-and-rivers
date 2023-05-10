package no.nav.arbeidsgiver.toi.arenafritattkandidatsok

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import java.time.LocalDate
import java.time.ZonedDateTime

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FritattRepositoryTest {

    lateinit var repository: FritattRepository

    @BeforeAll
    fun beforeALl() {
        repository = kandidatlisteRepositoryMedLokalPostgres()
    }

    @AfterEach
    fun afterEach() {
        slettAllDataIDatabase()
    }

    @Test
    fun opprettFritatt() {
        val fnr = "12345678910"
        val startdato = LocalDate.now().minusDays(1)
        val sluttdato = LocalDate.now().plusDays(1)
        val sistEndretIArena = ZonedDateTime.now()
        val slettetIArena = false
        val meldingFraArena = """{}"""
        val opprettetRad = ZonedDateTime.now().minusDays(1)
        val sistEndretRad = ZonedDateTime.now().minusHours(1)
        val fritatt = Fritatt.fraDatabase(
            1,
            fnr,
            startdato,
            sluttdato,
            sistEndretIArena,
            slettetIArena,
            meldingFraArena,
            opprettetRad,
            sistEndretRad
        )
        repository.upsertFritatt(fritatt)
        hentAlle().apply {
            assertThat(this).hasSize(1)
            first().also {
                assertThat(it.fnr).isEqualTo(fnr)
                assertThat(it.startdato).isEqualTo(startdato)
                assertThat(it.sluttdato).isEqualTo(sluttdato)
                assertThat(it.sistEndretIArena).isEqualToIgnoringNanos(sistEndretIArena)
                assertThat(it.slettetIArena).isEqualTo(slettetIArena)
                assertThat(it.meldingFraArena).isEqualTo(meldingFraArena)
                assertThat(it.opprettetRad).isEqualToIgnoringNanos(opprettetRad)
                assertThat(it.sistEndretRad).isEqualToIgnoringNanos(sistEndretRad)
            }
        }
    }

    @Test
    fun `Om man upserter en til rad med samme fnr skal den gamle raden oppdateres`() {
        val fnr = "12345678910"
        val startdato = LocalDate.now().minusDays(1)
        val sluttdato = LocalDate.now().plusDays(1)
        val sistEndretIArena = ZonedDateTime.now()
        val slettetIArena = false
        val meldingFraArena = """{}"""
        val gammelRadOppdatert = ZonedDateTime.now().minusDays(3)
        val nyRadOppdatert = ZonedDateTime.now().minusHours(3)
        repository.upsertFritatt(
            Fritatt.fraDatabase(
                1,
                fnr,
                startdato.minusMonths(1),
                sluttdato.minusMonths(1),
                sistEndretIArena.minusDays(2),
                false,
                """{"noe":"annet"}""",
                gammelRadOppdatert,
                gammelRadOppdatert
            )
        )
        val id = hentAlle().first().id
        val fritatt = Fritatt.fraDatabase(
            42,
            fnr,
            startdato,
            sluttdato,
            sistEndretIArena,
            slettetIArena,
            meldingFraArena,
            nyRadOppdatert,
            nyRadOppdatert
        )
        repository.upsertFritatt(fritatt)
        hentAlle().apply {
            assertThat(this).hasSize(1)
            first().also {
                assertThat(it.id).isEqualTo(id)
                assertThat(it.fnr).isEqualTo(fnr)
                assertThat(it.startdato).isEqualTo(startdato)
                assertThat(it.sluttdato).isEqualTo(sluttdato)
                assertThat(it.sistEndretIArena).isEqualToIgnoringNanos(sistEndretIArena)
                assertThat(it.slettetIArena).isEqualTo(slettetIArena)
                assertThat(it.meldingFraArena).isEqualTo(meldingFraArena)
                assertThat(it.opprettetRad).isEqualToIgnoringNanos(gammelRadOppdatert)
                assertThat(it.sistEndretRad).isEqualToIgnoringNanos(nyRadOppdatert)
            }
        }
    }

    @Test
    fun `Om statuskallet feiler skal fritatt raden rulles tilbake`() {
        slettStatusTabell()

        val fnr = "12345678910"
        val startdato = LocalDate.now().minusDays(1)
        val sluttdato = LocalDate.now().plusDays(1)
        val sistEndretIArena = ZonedDateTime.now()
        val slettetIArena = false
        val meldingFraArena = """{}"""
        val opprettetRad = ZonedDateTime.now().minusDays(1)
        val sistEndretRad = ZonedDateTime.now().minusHours(1)
        val fritatt = Fritatt.fraDatabase(
            1,
            fnr,
            startdato,
            sluttdato,
            sistEndretIArena,
            slettetIArena,
            meldingFraArena,
            opprettetRad,
            sistEndretRad
        )
        assertThrows<Exception> { repository.upsertFritatt(fritatt) }
        hentAlle().apply {
            assertThat(this).isEmpty()
        }
    }

}