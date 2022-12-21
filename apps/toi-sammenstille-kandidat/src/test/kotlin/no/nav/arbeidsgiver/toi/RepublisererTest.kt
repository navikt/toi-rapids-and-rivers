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
    private lateinit var javalin: Javalin

    @BeforeEach
    fun before() {
        javalin = Javalin.create().start(9000)
    }

    @AfterEach
    fun after() {
        javalin.stop()
        testDatabase.slettAlt()
    }

    @Test
    fun `Kall til republiseringsendepunkt skal returnere 200 og sende alle sammenstilte kandidater på rapiden`() {
        val testRapid = TestRapid()
        startApp(testRapid, TestDatabase().dataSource, javalin, riktigPassord)

        val lagredeKandidater = lagre3KandidaterTilDatabasen(Repository(testDatabase.dataSource))
        val body = Republiserer.RepubliseringBody(passord = riktigPassord)

        val response = Fuel.post("http://localhost:9000/republiser")
            .jsonBody(jacksonObjectMapper().writeValueAsString(body)).response().second

        assertThat(response.statusCode).isEqualTo(200)

        Thread.sleep(200) // Pga. asynkron håndtering av republisering
        val inspektør = testRapid.inspektør
        assertThat(inspektør.size).isEqualTo(lagredeKandidater.size)

        lagredeKandidater.forEachIndexed { index, kandidat ->
            assertThat(Kandidat.fraJson(inspektør.message(index)).toJson()).isEqualTo(kandidat.toJson())
            assertThat(inspektør.message(index).get("@event_name").asText()).isEqualTo("republisert.sammenstilt")
        }
    }

    @Test
    fun `Kall til republiseringsendepunkt skal fungere for flere hundre kandidater`() {
        val testRapid = TestRapid()
        startApp(testRapid, TestDatabase().dataSource, javalin, riktigPassord)

        val lagredeKandidater = lagreNKandidaterTilDatabasen(Repository(testDatabase.dataSource), 350)

        val body = Republiserer.RepubliseringBody(passord = riktigPassord)
        val response = Fuel.post("http://localhost:9000/republiser")
            .jsonBody(jacksonObjectMapper().writeValueAsString(body)).response().second
        assertThat(response.statusCode).isEqualTo(200)

        Thread.sleep(2000)
        val inspektør = testRapid.inspektør
        assertThat(inspektør.size).isEqualTo(lagredeKandidater.size)

        val aktørIderPåRapid = List(lagredeKandidater.size) { index -> Kandidat.fraJson(inspektør.message(index)).aktørId }
        assertThat(lagredeKandidater).containsExactlyInAnyOrder(*aktørIderPåRapid.toTypedArray())
    }

    @Test
    fun `Kall til republiseringsendepunkt for én kandidat skal sende kandidaten på rapid`() {
        val testRapid = TestRapid()
        startApp(testRapid, TestDatabase().dataSource, javalin, riktigPassord)

        val lagredeKandidater = lagreNKandidaterTilDatabasen(Repository(testDatabase.dataSource), 350)
        val aktørIdTilKandidatSomSkalRepubliseres = lagredeKandidater[13]

        val body = Republiserer.RepubliseringBody(passord = riktigPassord)
        val response = Fuel.post("http://localhost:9000/republiser/$aktørIdTilKandidatSomSkalRepubliseres")
            .jsonBody(jacksonObjectMapper().writeValueAsString(body)).response().second
        assertThat(response.statusCode).isEqualTo(200)

        val inspektør = testRapid.inspektør
        assertThat(inspektør.size).isEqualTo(1)
        assertThat(Kandidat.fraJson(inspektør.message(0)).aktørId).isEqualTo(aktørIdTilKandidatSomSkalRepubliseres)
        assertThat(inspektør.message(0).get("@event_name").asText()).isEqualTo("republisert.sammenstilt")
    }

    @Test
    fun `Kall til republiseringsendepunkt for en kandidat som ikke finnes returnerer 404 og sender ikke melding på rapid`() {
        val testRapid = TestRapid()
        startApp(testRapid, TestDatabase().dataSource, javalin, riktigPassord)

        lagreNKandidaterTilDatabasen(Repository(testDatabase.dataSource), 350)
        val ugyldigAktørId = "enUgyldigAktørId"

        val body = Republiserer.RepubliseringBody(passord = riktigPassord)
        val response = Fuel.post("http://localhost:9000/republiser/$ugyldigAktørId")
            .jsonBody(jacksonObjectMapper().writeValueAsString(body)).response().second
        assertThat(response.statusCode).isEqualTo(404)

        val inspektør = testRapid.inspektør
        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `Kall til republiseringsendepunkt med feil passord skal returnere 401 og ikke republisere noen kandidater`() {
        val testRapid = TestRapid()
        startApp(testRapid, TestDatabase().dataSource, javalin, riktigPassord)

        lagre3KandidaterTilDatabasen(Repository(testDatabase.dataSource))

        val feilPassord = "jalla"
        val body = Republiserer.RepubliseringBody(passord = feilPassord)
        val response = Fuel.post("http://localhost:9000/republiser")
            .jsonBody(jacksonObjectMapper().writeValueAsString(body)).response().second

        assertThat(response.statusCode).isEqualTo(401)

        val inspektør = testRapid.inspektør
        assertThat(inspektør.size).isEqualTo(0)
    }

    private fun lagre3KandidaterTilDatabasen(repository: Repository) =
        listOf("111", "222", "333").map {
            val kandidat = Kandidat.fraJson(arbeidsmarkedCvMelding(it))
            repository.lagreKandidat(kandidat)
            kandidat
        }

    private fun lagreNKandidaterTilDatabasen(repository: Repository, n: Int): List<String> {
        var count = 0
        return generateSequence { (count++).takeIf { it < n } }
            .toList()
            .map {
                val kandidat = Kandidat.fraJson(arbeidsmarkedCvMelding(it.toString()))
                repository.lagreKandidat(kandidat)
                kandidat.aktørId
            }
    }
}
