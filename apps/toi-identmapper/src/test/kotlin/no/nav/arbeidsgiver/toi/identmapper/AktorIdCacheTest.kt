package no.nav.arbeidsgiver.toi.identmapper

import TestDatabase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class AktorIdCacheTest {
    val testDatabase: TestDatabase = TestDatabase()

    @AfterEach
    fun slettDatabase() {
        testDatabase.slettAlt()
    }

    @Test
    fun `Henting av aktørId skal returnere aktørId fra PDL når det ikke finnes i databasen`() {
        // Arrange
        val fødselsnummer = "123"
        val aktørIdFraPdl = "456"

        val repository = Repository(testDatabase.dataSource)
        val aktørIdCache = AktorIdCache(repository) { aktørIdFraPdl }

        assertThat(repository.hentIdentMappinger(fødselsnummer)).isEmpty()

        // Act
        val hentetAktørId = aktørIdCache.hentAktørId(fødselsnummer)

        // Assert
        assertThat(hentetAktørId).isEqualTo(aktørIdFraPdl)
    }

    @Test
    fun `Henting av aktørId fra PDL skal lagre identmapping i databasen`() {
        // Arrange
        val fødselsnummer = "123"
        val aktørIdFraPdl = "456"
        val repository = Repository(testDatabase.dataSource)
        val aktørIdCache = AktorIdCache(repository) { aktørIdFraPdl }

        // Act
        aktørIdCache.hentAktørId(fødselsnummer)

        // Assert
        val mappinger = repository.hentIdentMappinger(fødselsnummer)

        assertThat(mappinger.size).isEqualTo(1)

        mappinger.first().apply {
            assertThat(this.aktørId).isEqualTo(aktørIdFraPdl)
            assertThat(this.fødselsnummer).isEqualTo(fødselsnummer)
            assertThat(this.cachetTidspunkt).isEqualToIgnoringMinutes(LocalDateTime.now())
        }
    }

    @Test
    fun `Henting av aktørId skal returnere aktørId-en som er lagret i databasen og ikke hente på nytt fra PDL`() {
        // Arrange
        val fødselsnummer = "123"
        val aktørIdIDatabasen = "789"

        val repository = Repository(testDatabase.dataSource)
        val aktørIdCache = AktorIdCache(repository) { "dummyAktørIdFraPdlSomIkkeSkalBrukes" }

        testDatabase.lagreIdentMapping(
            IdentMapping(
                fødselsnummer = fødselsnummer,
                aktørId = aktørIdIDatabasen,
                cachetTidspunkt = LocalDateTime.now()
            )
        )

        // Act
        val hentetAktørId = aktørIdCache.hentAktørId(fødselsnummer)

        // Assert
        assertThat(hentetAktørId).isEqualTo(aktørIdIDatabasen)
    }


    @Test
    fun `Henting av aktørId skal returnere nyeste når det finnes to eller flere ulike aktørId-er`() {
        // Arrange
        val fødselsnummer = "123"
        val nyesteAktøridIDatabasen = "456"
        val eldsteAktørIdIDatabasen = "789"

        val repository = Repository(testDatabase.dataSource)
        val aktørIdCache = AktorIdCache(repository) { "dummyAktørIdSomIkkeSkalHentes" }

        val nyesteIdentMapping = IdentMapping(
            fødselsnummer = fødselsnummer,
            aktørId = nyesteAktøridIDatabasen,
            cachetTidspunkt = LocalDateTime.now()
        )
        val eldsteIdentMapping = IdentMapping(
            fødselsnummer = fødselsnummer,
            aktørId = eldsteAktørIdIDatabasen,
            cachetTidspunkt = nyesteIdentMapping.cachetTidspunkt.minusHours(1)
        )
        testDatabase.lagreIdentMapping(nyesteIdentMapping)
        testDatabase.lagreIdentMapping(eldsteIdentMapping)

        // Act
        val hentetAktørId = aktørIdCache.hentAktørId(fødselsnummer)

        // Assert
        assertThat(hentetAktørId).isEqualTo(nyesteAktøridIDatabasen)
    }
}