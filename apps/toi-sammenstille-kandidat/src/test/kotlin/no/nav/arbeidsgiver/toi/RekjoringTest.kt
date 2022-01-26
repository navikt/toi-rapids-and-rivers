package no.nav.arbeidsgiver.toi

import TestDatabase
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import cvMelding
import modifiserbareSystemVariabler
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RekjoringTest {

    private val testDatabase = TestDatabase()
    private val repository = Repository(testDatabase.dataSource)

    @BeforeEach
    fun before() {
        modifiserbareSystemVariabler["NAIS_APP_NAME"] = "toi-sammenstiller" // TODO: Trenger vi denne?
    }

    @AfterEach
    fun after() {
        testDatabase.slettAlt()
    }

    @Test
    fun `rekjøring ende-til-ende`() {
        val lagredeKandidater = lagre3KandidaterTilDatabasen()
        val rekjøringspassord = "passord"
        val testRapid = TestRapid()
        KandidatRekjorer(rekjøringspassord, repository, testRapid)
        val body = KandidatRekjorer.RekjøringBody(passord = rekjøringspassord)

        Fuel.post("http://localhost:9031/republiser-kandidater")
            .jsonBody(jacksonObjectMapper().writeValueAsString(body)).response()

        val inspektør = testRapid.inspektør
        assertThat(inspektør.size).isEqualTo(lagredeKandidater.size)

        lagredeKandidater.forEachIndexed { index, kandidat ->
            assertThat(Kandidat.fraJson(inspektør.message(index)).toJson()).isEqualTo(kandidat.toJson())
        }
    }

    /*
    @Test
    fun `Ingen rekjøring dersom db er tom`() {
        val kjøring = Rekjoring(repo, rapid)
        kjøring.rekjør()

        val inspektør = rapid.inspektør

        assertThat(inspektør.size).isZero
    }

    @Test
    fun `Legg alle kandidater på rapid`() {
        val repo = Repository(testDatabase.dataSource)
        val kandidat1 = Kandidat.fraJson(cvMelding("111"))
        val kandidat2 = Kandidat.fraJson(cvMelding("222"))
        val kandidat3 = Kandidat.fraJson(cvMelding("333"))
        repo.lagreKandidat(kandidat1)
        repo.lagreKandidat(kandidat2)
        repo.lagreKandidat(kandidat3)

        val rapid = TestRapid()
        val kjøring = Rekjoring(repo, rapid)

        kjøring.rekjør()

        val inspektør = rapid.inspektør

        assertThat(inspektør.size).isEqualTo(3)
        assertThat(Kandidat.fraJson(inspektør.message(0)).toJson()).isEqualTo(kandidat1.toJson())
        assertThat(Kandidat.fraJson(inspektør.message(1)).toJson()).isEqualTo(kandidat2.toJson())
        assertThat(Kandidat.fraJson(inspektør.message(2)).toJson()).isEqualTo(kandidat3.toJson())
    }


     */
    private fun lagre3KandidaterTilDatabasen(): List<Kandidat> {
        val kandidat1 = Kandidat.fraJson(cvMelding("111"))
        val kandidat2 = Kandidat.fraJson(cvMelding("222"))
        val kandidat3 = Kandidat.fraJson(cvMelding("333"))
        repository.lagreKandidat(kandidat1)
        repository.lagreKandidat(kandidat2)
        repository.lagreKandidat(kandidat3)
        return listOf(kandidat1, kandidat2, kandidat3)
    }
}