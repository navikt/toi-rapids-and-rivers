package no.nav.arbeidsgiver.toi.identmapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class IdentRepositoryTest {
    private val testDatabase = TestDatabase()

    @BeforeEach
    fun kjørFlyway() {
        IdentRepository(testDatabase.dataSource).kjørFlywayMigreringer()
    }

    @AfterEach
    fun slettDatabase() {
        testDatabase.slettAlt()
    }

    @Test
    fun `lagreIdentMapping skal oppdatere cachet_tidspunkt når mapping finnes`() {
        val fødselsnummer = "123"
        val aktørId = "456"
        val repository = IdentRepository(testDatabase.dataSource)

        val gammeltTidspunkt = LocalDateTime.now().minusHours(1)
        testDatabase.lagreIdentMapping(
            IdentMapping(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                cachetTidspunkt = gammeltTidspunkt
            )
        )

        repository.lagreIdentMapping(aktørId, fødselsnummer)

        val mappinger = repository.hentIdentMappingerForAktørId(aktørId)
        assertThat(mappinger).hasSize(1)
        assertThat(mappinger.first().cachetTidspunkt).isAfter(gammeltTidspunkt)
    }

    @Test
    fun `hentIdentMappingerForAktørId skal hente mappinger for gitt aktørId`() {
        val forventetFnr = "123"
        val aktørId = "456"
        val annenAktørId = "789"

        testDatabase.lagreIdentMapping(
            IdentMapping(
                aktørId = aktørId,
                fødselsnummer = forventetFnr,
                cachetTidspunkt = LocalDateTime.now()
            )
        )
        testDatabase.lagreIdentMapping(
            IdentMapping(
                aktørId = annenAktørId,
                fødselsnummer = "999",
                cachetTidspunkt = LocalDateTime.now()
            )
        )

        val repository = IdentRepository(testDatabase.dataSource)
        val mappinger = repository.hentIdentMappingerForAktørId(aktørId)

        assertThat(mappinger).hasSize(1)
        assertThat(mappinger.first().fødselsnummer).isEqualTo(forventetFnr)
    }
}
