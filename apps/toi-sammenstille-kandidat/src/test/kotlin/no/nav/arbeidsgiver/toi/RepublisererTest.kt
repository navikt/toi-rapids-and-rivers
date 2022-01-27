package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import io.javalin.Javalin
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RepublisererTest {
    private val riktigPassord = "passord"
    private val testDatabase = TestDatabase()
    private val repository = Repository(testDatabase.dataSource)
    private lateinit var javalin: Javalin

    @BeforeEach
    fun before() {
        modifiserbareSystemVariabler["NAIS_APP_NAME"] = "toi-sammenstiller"

        if (this::javalin.isInitialized) {
            javalin.stop()
        }
        javalin = Javalin.create().start(9000)
    }

    @AfterEach
    fun after() {
        testDatabase.slettAlt()
    }

    @Test
    fun `Kall til republiseringsendepunkt skal returnere 200 og sende alle sammenstilte kandidater på rapiden`() {
        val testRapid = TestRapid()
        startApp(testRapid, TestDatabase().dataSource, javalin, riktigPassord)

        val lagredeKandidater = lagre3KandidaterTilDatabasen(Repository(testDatabase.dataSource))
        val body = Republiserer.RepubliseringBody(passord = riktigPassord)

        val response = Fuel.post("http://localhost:9001/republiserKandidater")
            .jsonBody(jacksonObjectMapper().writeValueAsString(body)).response().second

        assertThat(response.statusCode).isEqualTo(200)

        Thread.sleep(200) // Pga. asynkron håndtering av republisering
        val inspektør = testRapid.inspektør
        assertThat(inspektør.size).isEqualTo(lagredeKandidater.size)

        lagredeKandidater.forEachIndexed { index, kandidat ->
            assertThat(Kandidat.fraJson(inspektør.message(index)).toJson()).isEqualTo(kandidat.toJson())
        }
    }

    @Test
    fun `Kall til republiseringsendepunkt med feil passord skal returnere 401 og ikke republisere noen kandidater`() {
        val testRapid = TestRapid()
        startApp(testRapid, TestDatabase().dataSource, javalin, riktigPassord)

        lagre3KandidaterTilDatabasen(Repository(testDatabase.dataSource))

        val feilPassord = "jalla"
        val body = Republiserer.RepubliseringBody(passord = feilPassord)

        val response = Fuel.post("http://localhost:9001/republiserKandidater")
            .jsonBody(jacksonObjectMapper().writeValueAsString(body)).response().second

        assertThat(response.statusCode).isEqualTo(401)

        val inspektør = testRapid.inspektør
        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `Kall til republiseringsendepunkt skal stoppe rapid og starte den igjen når republisering er ferdig`() {
        val testRapid = TestRapid()
        val repository = Repository(testDatabase.dataSource)
        startApp(testRapid, TestDatabase().dataSource, javalin, riktigPassord)

        lagre3KandidaterTilDatabasen(repository)

        val body = Republiserer.RepubliseringBody(passord = riktigPassord)
        val response = Fuel.post("http://localhost:9002/republiserKandidater")
            .jsonBody(jacksonObjectMapper().writeValueAsString(body)).response().second
        assertThat(response.statusCode).isEqualTo(200)

        testRapid.sendTestMessage(veilederMelding("111"))
        assertThat(testRapid.inspektør.size).isEqualTo(0)

        // Jobben er ferdig




    }

    private fun lagre3KandidaterTilDatabasen(repository: Repository) =
        listOf("111", "222", "333").map {
            val kandidat = Kandidat.fraJson(cvMelding(it))
            repository.lagreKandidat(kandidat)
            kandidat
        }
}
