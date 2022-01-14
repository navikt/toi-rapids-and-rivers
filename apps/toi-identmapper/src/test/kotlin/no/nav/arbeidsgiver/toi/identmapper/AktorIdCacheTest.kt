package no.nav.arbeidsgiver.toi.identmapper

import TestDatabase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.time.LocalDateTime

class AktorIdCacheTest {
    private val testDatabase: TestDatabase = TestDatabase()

    @BeforeEach
    fun kjørFlyway() {
        Repository(testDatabase.dataSource).kjørFlywayMigreringer()
    }

    @AfterEach
    fun slettDatabase() {
        testDatabase.slettAlt()
    }
    
    @Test
    fun `Henting av aktørId skal returnere aktørId fra PDL når det ikke finnes i databasen`() {
        val fødselsnummer = "123"
        val aktørIdFraPdl = "456"

        val repository = Repository(testDatabase.dataSource)
        val aktørIdCache = AktorIdCache(repository, false) { aktørIdFraPdl }

        assertThat(repository.hentIdentMappinger(fødselsnummer)).isEmpty()

        val hentetAktørId = aktørIdCache.hentAktørId(fødselsnummer)
        assertThat(hentetAktørId).isEqualTo(aktørIdFraPdl)
    }

    @Test
    fun `Henting av aktørId fra PDL skal lagre identmapping i databasen`() {
        val fødselsnummer = "123"
        val aktørIdFraPdl = "456"
        val repository = Repository(testDatabase.dataSource)
        val aktørIdCache = AktorIdCache(repository, false) { aktørIdFraPdl }

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
        val aktørIdCache = AktorIdCache(repository, false) { "dummyAktørIdFraPdlSomIkkeSkalBrukes" }

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
    fun `Henting av aktørId skal lagre null-aktørid i database for dev-gcp`() {
        val fødselsnummer = "123"
        val aktørIdIPDL = null

        val repository = Repository(testDatabase.dataSource)
        val aktørIdCache = AktorIdCache(repository, true) { aktørIdIPDL }

        val hentetAktørId = aktørIdCache.hentAktørId(fødselsnummer)

        assertThat(hentetAktørId).isEqualTo(aktørIdIPDL)
        assertThat(repository.hentIdentMappinger(fødselsnummer).size).isEqualTo(1)
        assertThat(repository.hentIdentMappinger(fødselsnummer).first().aktørId).isNull()
    }

    @Test
    fun `Henting av aktørId skal ikke lagre null-aktørid i database for prod-gcp`() {
        val fødselsnummer = "123"
        val aktørIdIPDL = null

        val repository = Repository(testDatabase.dataSource)
        val aktørIdCache = AktorIdCache(repository, false) { aktørIdIPDL }

        val hentetAktørId = aktørIdCache.hentAktørId(fødselsnummer)

        assertThat(hentetAktørId).isEqualTo(aktørIdIPDL)
        assertThat(repository.hentIdentMappinger(fødselsnummer).size).isEqualTo(0)
    }

    @Test
    fun `Henting av aktørId skal returnere aktørId-en som er lagret i databasen, selv om den er null`() {
        val fødselsnummer = "123"
        val aktørIdIDatabasen = null

        val repository = Repository(testDatabase.dataSource)
        val aktørIdCache = AktorIdCache(repository, true) { "dummyAktørIdFraPdlSomIkkeSkalBrukes" }

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
    fun `Henting av aktørId skal returnere nyeste når det finnes to eller flere ulike aktørId-er`() {
        val fødselsnummer = "123"
        val nyesteAktøridIDatabasen = "456"
        val eldsteAktørIdIDatabasen = "789"

        val repository = Repository(testDatabase.dataSource)
        val aktørIdCache = AktorIdCache(repository, false) { "dummyAktørIdSomIkkeSkalHentes" }

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
