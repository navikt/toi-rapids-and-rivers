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
        val fødselsnummer = "123"
        val aktørIdFraPdl = "456"

        val repository = Repository(testDatabase.dataSource)
        val aktørIdCache = AktorIdCache(repository) { aktørIdFraPdl }

        assertThat(repository.hentIdentMappinger(fødselsnummer)).isEmpty()

        val hentetAktørId = aktørIdCache.hentAktørId(fødselsnummer)
        assertThat(hentetAktørId).isEqualTo(aktørIdFraPdl)
    }

    @Test
    fun `Henting av aktørId fra PDL skal lagre identmapping i databasen`() {
        val fødselsnummer = "123"
        val aktørIdFraPdl = "456"
        val repository = Repository(testDatabase.dataSource)
        val aktørIdCache = AktorIdCache(repository) { aktørIdFraPdl }

        aktørIdCache.hentAktørId(fødselsnummer)

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
        assertThat(repository.hentIdentMappinger(fødselsnummer).size).isEqualTo(1)

        val hentetAktørId = aktørIdCache.hentAktørId(fødselsnummer)

        assertThat(hentetAktørId).isEqualTo(aktørIdIDatabasen)
        assertThat(repository.hentIdentMappinger(fødselsnummer).size).isEqualTo(1)
    }

    @Test
    fun `Henting av aktørId skal returnere aktørId fra PDL hvis siste mapping som er lagret i databasen er utgått`() {
        val fødselsnummer = "123"
        val aktørIdFraPdl = "456"
        val aktørIdIDatabasen = "789"

        val repository = Repository(testDatabase.dataSource)
        val aktørIdCache = AktorIdCache(repository) { aktørIdFraPdl }

        testDatabase.lagreIdentMapping(
            IdentMapping(
                fødselsnummer = fødselsnummer,
                aktørId = aktørIdIDatabasen,
                cachetTidspunkt = LocalDateTime.now().minusDays(31)
            )
        )

        assertThat(repository.hentIdentMappinger(fødselsnummer).size).isEqualTo(1)

        val hentetAktørId = aktørIdCache.hentAktørId(fødselsnummer)

        assertThat(hentetAktørId).isEqualTo(aktørIdFraPdl)
        assertThat(repository.hentIdentMappinger(fødselsnummer).size).isEqualTo(2)
    }


    @Test
    fun `Henting av aktørId skal returnere nyeste når det finnes to eller flere ulike aktørId-er`() {
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

        val hentetAktørId = aktørIdCache.hentAktørId(fødselsnummer)

        assertThat(hentetAktørId).isEqualTo(nyesteAktøridIDatabasen)
    }
}