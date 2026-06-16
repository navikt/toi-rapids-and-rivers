package no.nav.arbeidsgiver.toi.identmapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.time.LocalDateTime

class IdentCacheTest {
    private val testDatabase: TestDatabase = TestDatabase()

    @BeforeEach
    fun kjørFlyway() {
        IdentRepository(testDatabase.dataSource).kjørFlywayMigreringer()
    }

    @AfterEach
    fun slettDatabase() {
        testDatabase.slettAlt()
    }

    @Test
    fun `Henting av aktørId skal returnere aktørId fra PDL når det ikke finnes i databasen`() {
        val fødselsnummer = "123"
        val aktørIdFraPdl = "456"

        val repository = IdentRepository(testDatabase.dataSource)
        val identCache = IdentCache(repository, false, { aktørIdFraPdl }, { fail("Skal ikke hente fødselsnummer fra PDL") })

        assertThat(repository.hentIdentMappingerForFnr(fødselsnummer)).isEmpty()

        val hentetAktørId = identCache.hentAktørId(fødselsnummer)
        assertThat(hentetAktørId).isEqualTo(aktørIdFraPdl)
    }

    @Test
    fun `Henting av aktørId fra PDL skal lagre identmapping i databasen`() {
        val fødselsnummer = "123"
        val aktørIdFraPdl = "456"
        val repository = IdentRepository(testDatabase.dataSource)
        val identCache = IdentCache(repository, false, { aktørIdFraPdl }, { fail("Skal ikke hente fødselsnummer fra PDL") })

        identCache.hentAktørId(fødselsnummer)

        val mappinger = repository.hentIdentMappingerForFnr(fødselsnummer)

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

        val repository = IdentRepository(testDatabase.dataSource)
        val identCache = IdentCache(
            repository,
            false,
            { fail("Skal ikke hente aktørId fra PDL") },
            { fail("Skal ikke hente fødselsnummer fra PDL") }
        )

        testDatabase.lagreIdentMapping(
            IdentMapping(
                fødselsnummer = fødselsnummer,
                aktørId = aktørIdIDatabasen,
                cachetTidspunkt = LocalDateTime.now()
            )
        )
        assertThat(repository.hentIdentMappingerForFnr(fødselsnummer).size).isEqualTo(1)

        val hentetAktørId = identCache.hentAktørId(fødselsnummer)

        assertThat(hentetAktørId).isEqualTo(aktørIdIDatabasen)
        assertThat(repository.hentIdentMappingerForFnr(fødselsnummer).size).isEqualTo(1)
    }

    @Test
    fun `Henting av aktørId skal lagre null-aktørid i database for dev-gcp`() {
        val fødselsnummer = "123"
        val aktørIdIPDL = null

        val repository = IdentRepository(testDatabase.dataSource)
        val identCache = IdentCache(repository, true, { aktørIdIPDL }, { fail("Skal ikke hente fødselsnummer fra PDL") })

        val hentetAktørId = identCache.hentAktørId(fødselsnummer)

        assertThat(hentetAktørId).isEqualTo(aktørIdIPDL)
        assertThat(repository.hentIdentMappingerForFnr(fødselsnummer).size).isEqualTo(1)
        assertThat(repository.hentIdentMappingerForFnr(fødselsnummer).first().aktørId).isNull()
    }

    @Test
    fun `Henting av aktørId skal ikke lagre null-aktørid i database for prod-gcp`() {
        val fødselsnummer = "123"
        val aktørIdIPDL = null

        val repository = IdentRepository(testDatabase.dataSource)
        val identCache = IdentCache(repository, false, { aktørIdIPDL }, { fail("Skal ikke hente fødselsnummer fra PDL") })

        val hentetAktørId = identCache.hentAktørId(fødselsnummer)

        assertThat(hentetAktørId).isEqualTo(aktørIdIPDL)
        assertThat(repository.hentIdentMappingerForFnr(fødselsnummer).size).isEqualTo(0)
    }

    @Test
    fun `Henting av aktørId skal returnere aktørId-en som er lagret i databasen, selv om den er null`() {
        val fødselsnummer = "123"
        val aktørIdIDatabasen = null

        val repository = IdentRepository(testDatabase.dataSource)
        val identCache = IdentCache(
            repository,
            true,
            { fail("Skal ikke hente aktørId fra PDL") },
            { fail("Skal ikke hente fødselsnummer fra PDL") }
        )

        testDatabase.lagreIdentMapping(
            IdentMapping(
                fødselsnummer = fødselsnummer,
                aktørId = aktørIdIDatabasen,
                cachetTidspunkt = LocalDateTime.now()
            )
        )
        assertThat(repository.hentIdentMappingerForFnr(fødselsnummer).size).isEqualTo(1)

        val hentetAktørId = identCache.hentAktørId(fødselsnummer)

        assertThat(hentetAktørId).isEqualTo(aktørIdIDatabasen)
        assertThat(repository.hentIdentMappingerForFnr(fødselsnummer).size).isEqualTo(1)
    }

    @Test
    fun `Henting av aktørId skal returnere nyeste når det finnes to eller flere ulike aktørId-er`() {
        val fødselsnummer = "123"
        val nyesteAktøridIDatabasen = "456"
        val eldsteAktørIdIDatabasen = "789"

        val repository = IdentRepository(testDatabase.dataSource)
        val identCache = IdentCache(
            repository,
            false,
            { fail("Skal ikke hente aktørId fra PDL") },
            { fail("Skal ikke hente fødselsnummer fra PDL") }
        )

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

        val hentetAktørId = identCache.hentAktørId(fødselsnummer)

        assertThat(hentetAktørId).isEqualTo(nyesteAktøridIDatabasen)
    }

    @Test
    fun `Henting av fødselsnummer skal hente fra PDL og cache ved cache miss`() {
        val aktørId = "456"
        val fødselsnummerFraPdl = "123"
        val repository = IdentRepository(testDatabase.dataSource)
        val identCache = IdentCache(
            repository,
            false,
            { fail("Skal ikke hente aktørId fra PDL") },
            { fødselsnummerFraPdl }
        )

        val fødselsnummer = identCache.hentFødselsnummer(aktørId)

        assertThat(fødselsnummer).isEqualTo(fødselsnummerFraPdl)
        assertThat(repository.hentIdentMappingerForAktørId(aktørId)).hasSize(1)
    }

    @Test
    fun `Henting av fødselsnummer skal bruke cache når mapping finnes`() {
        val aktørId = "456"
        val fødselsnummer = "123"
        val repository = IdentRepository(testDatabase.dataSource)

        testDatabase.lagreIdentMapping(
            IdentMapping(
                fødselsnummer = fødselsnummer,
                aktørId = aktørId,
                cachetTidspunkt = LocalDateTime.now()
            )
        )

        val identCache = IdentCache(
            repository,
            false,
            { fail("Skal ikke hente aktørId fra PDL") },
            { fail("Skal ikke hente fødselsnummer fra PDL") }
        )

        val hentetFødselsnummer = identCache.hentFødselsnummer(aktørId)

        assertThat(hentetFødselsnummer).isEqualTo(fødselsnummer)
    }
}
