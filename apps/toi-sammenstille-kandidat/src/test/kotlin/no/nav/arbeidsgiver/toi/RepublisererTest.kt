package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RepublisererTest {
    private val riktigPassord = "passord"
    private val testDatabase = TestDatabase()
    private val repository = Repository(testDatabase.dataSource)
    private val testRapid = TestRapid()

    init {
        Republiserer(riktigPassord, repository, testRapid)
    }

    @BeforeEach
    fun before() {
        modifiserbareSystemVariabler["NAIS_APP_NAME"] = "toi-sammenstiller"
    }

    @AfterEach
    fun after() {
        testDatabase.slettAlt()
        testRapid.reset()
    }

    @Test
    fun `Kall til republiseringsendepunkt skal returnere 200 og sende alle sammenstilte kandidater på rapiden`() {
        val lagredeKandidater = lagre3KandidaterTilDatabasen()
        val body = Republiserer.RepubliseringBody(passord = riktigPassord)

        val response = Fuel.post("http://localhost:9031/republiserKandidater")
            .jsonBody(jacksonObjectMapper().writeValueAsString(body)).response().second

        assertThat(response.statusCode).isEqualTo(200)

        val inspektør = testRapid.inspektør
        assertThat(inspektør.size).isEqualTo(lagredeKandidater.size)

        lagredeKandidater.forEachIndexed { index, kandidat ->
            assertThat(Kandidat.fraJson(inspektør.message(index)).toJson()).isEqualTo(kandidat.toJson())
        }
    }

    @Test
    fun `Kall til republiseringsendepunkt med feil passord skal returnere 401 og ikke republisere noen kandidater`() {
        lagre3KandidaterTilDatabasen()

        val feilPassord = "jalla"
        val body = Republiserer.RepubliseringBody(passord = feilPassord)

        val response = Fuel.post("http://localhost:9031/republiserKandidater")
            .jsonBody(jacksonObjectMapper().writeValueAsString(body)).response().second

        assertThat(response.statusCode).isEqualTo(401)

        val inspektør = testRapid.inspektør
        assertThat(inspektør.size).isEqualTo(0)
    }

    private fun lagre3KandidaterTilDatabasen() =
        listOf("111", "222", "333").map {
            val kandidat = Kandidat.fraJson(cvMelding(it))
            repository.lagreKandidat(kandidat)
            kandidat
        }
}
