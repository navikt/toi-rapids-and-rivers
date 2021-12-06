package no.nav.arbeidsgiver.toi.identmapper

import TestDatabase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class AktorIdCacheTest {
    val database: TestDatabase = TestDatabase()

    @AfterEach
    fun slettDatabase() {
        database.slettAlt()
    }

    @Test
    fun `Henting av aktørId fra PDL skal lagre identmapping i databasen`() {
        val fødselsnummer = "123"
        val aktørIdFraPdl = "456"

        val repository = Repository(database.dataSource)
        val aktørIdCache = AktorIdCache(repository) { aktørIdFraPdl }

        assertThat(repository.hentIdentMappinger(fødselsnummer)).isEmpty()
        assertThat(aktørIdCache.hentAktørId(fødselsnummer)).isEqualTo(aktørIdFraPdl)

        val mappinger = repository.hentIdentMappinger(fødselsnummer)

        assertThat(mappinger.size).isEqualTo(1)

        mappinger.first().apply {
            assertThat(this.aktørId).isEqualTo(aktørIdFraPdl)
            assertThat(this.fødselsnummer).isEqualTo(fødselsnummer)
            assertThat(this.cachetTidspunkt).isEqualToIgnoringMinutes(LocalDateTime.now())
        }
    }
}