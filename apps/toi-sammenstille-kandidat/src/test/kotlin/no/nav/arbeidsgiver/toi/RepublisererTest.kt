package no.nav.arbeidsgiver.toi

import tools.jackson.module.kotlin.jacksonObjectMapper
import no.nav.toi.TestRapid
import io.javalin.Javalin
import no.nav.arbeidsgiver.toi.startApp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RepublisererTest {
    private val riktigPassord = "passord"
    private val testDatabase = TestDatabase()
    private val testRapid = TestRapid()
    private lateinit var app: AutoCloseable

    @BeforeEach
    fun before() {
        app = startApp(testRapid, TestDatabase().dataSource, 9000, riktigPassord)
    }

    @AfterEach
    fun after() {
        app.close()
        testRapid.reset()
        testDatabase.slettAlt()
    }

    @Test
    fun `Kall til republiseringsendepunkt skal returnere 200 og sende alle sammenstilte kandidaters aktørider på rapiden`() {
        val lagredeKandidater = lagre3KandidaterTilDatabasen(Repository(testDatabase.dataSource))
        val body = Republiserer.RepubliseringBody(passord = riktigPassord)

        val response = post("http://localhost:9000/republiser", jacksonObjectMapper().writeValueAsString(body))

        assertThat(response.statusCode()).isEqualTo(200)

        Thread.sleep(200) // Pga. asynkron håndtering av republisering
        val inspektør = testRapid.inspektør
        assertThat(inspektør.size).isEqualTo(lagredeKandidater.size)

        lagredeKandidater.forEachIndexed { index, kandidat ->
            assertThat(inspektør.message(index).get("@behov").first().asString()).isEqualTo("synlighet")
            assertThat(inspektør.message(index).get("aktørId").asString()).isEqualTo(kandidat.aktørId)
            assertThat(inspektør.message(index).get("@event_name").asString()).isEqualTo("republisert")
        }
    }

    @Test
    fun `Kall til republiseringsendepunkt skal fungere for flere hundre kandidater`() {
        val lagredeKandidater = lagreNKandidaterTilDatabasen(Repository(testDatabase.dataSource), 350)

        val body = Republiserer.RepubliseringBody(passord = riktigPassord)
        val response = post("http://localhost:9000/republiser", jacksonObjectMapper().writeValueAsString(body))
        assertThat(response.statusCode()).isEqualTo(200)

        Thread.sleep(2000)
        val inspektør = testRapid.inspektør
        assertThat(inspektør.size).isEqualTo(lagredeKandidater.size)

        val aktørIderPåRapid =
            List(lagredeKandidater.size) { index -> Kandidat.fraJson(inspektør.message(index)).aktørId }
        assertThat(lagredeKandidater).containsExactlyInAnyOrder(*aktørIderPåRapid.toTypedArray())
    }

    @Test
    fun `Kall til republiseringsendepunkt for én kandidat skal sende kandidaten på rapid`() {
        val lagredeKandidater = lagreNKandidaterTilDatabasen(Repository(testDatabase.dataSource), 350)
        val aktørIdTilKandidatSomSkalRepubliseres = lagredeKandidater[13]

        val body = Republiserer.RepubliseringBody(passord = riktigPassord)
        val response = post("http://localhost:9000/republiser/$aktørIdTilKandidatSomSkalRepubliseres", jacksonObjectMapper().writeValueAsString(body))
        assertThat(response.statusCode()).isEqualTo(200)

        val inspektør = testRapid.inspektør
        assertThat(inspektør.size).isEqualTo(1)
        assertThat(Kandidat.fraJson(inspektør.message(0)).aktørId).isEqualTo(aktørIdTilKandidatSomSkalRepubliseres)
        assertThat(inspektør.message(0).get("@event_name").asString()).isEqualTo("republisert")
    }

    @Test
    fun `Kall til republiseringsendepunkt for en liste av kandidater skal sende kandidaten på rapid`() {
        val lagredeKandidater = lagreNKandidaterTilDatabasen(Repository(testDatabase.dataSource), 350)
        val kandidat1 = lagredeKandidater[13]
        val kandidat2 = lagredeKandidater[15]
        val kandidat3 = lagredeKandidater[29]

        val body = Republiserer.RepubliseringBodyMedListeAvKandidater(
            passord = riktigPassord,
            aktorIder = listOf(kandidat1, kandidat2, kandidat3)
        )
        val body1 = jacksonObjectMapper().writeValueAsString(body)
        val response = post("http://localhost:9000/republiser/liste", body1)
        assertThat(response.statusCode()).isEqualTo(200)

        val inspektør = testRapid.inspektør
        assertThat(inspektør.size).isEqualTo(3)
        assertThat(Kandidat.fraJson(inspektør.message(0)).aktørId).isEqualTo(kandidat1)
        assertThat(inspektør.message(0).get("@event_name").asString()).isEqualTo("republisert")
        assertThat(Kandidat.fraJson(inspektør.message(1)).aktørId).isEqualTo(kandidat2)
        assertThat(inspektør.message(1).get("@event_name").asString()).isEqualTo("republisert")
        assertThat(Kandidat.fraJson(inspektør.message(2)).aktørId).isEqualTo(kandidat3)
        assertThat(inspektør.message(2).get("@event_name").asString()).isEqualTo("republisert")
    }

    @Test
    fun `Kall til republiseringsendepunkt for en kandidat som ikke finnes returnerer 404 og sender ikke melding på rapid`() {
        lagreNKandidaterTilDatabasen(Repository(testDatabase.dataSource), 350)
        val ugyldigAktørId = "enUgyldigAktørId"

        val body = Republiserer.RepubliseringBody(passord = riktigPassord)
        val response = post("http://localhost:9000/republiser/$ugyldigAktørId", jacksonObjectMapper().writeValueAsString(body))
        assertThat(response.statusCode()).isEqualTo(404)

        val inspektør = testRapid.inspektør
        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `Kall til republiseringsendepunkter med feil passord skal returnere 401 og ikke republisere noen kandidater`() {
        lagre3KandidaterTilDatabasen(Repository(testDatabase.dataSource))

        val feilPassord = "jalla"
        val body = Republiserer.RepubliseringBody(passord = feilPassord)
        listOf("republiser", "republiser/liste", "republiser/111").forEach { path ->
            val response = post("http://localhost:9000/$path", jacksonObjectMapper().writeValueAsString(body))

            assertThat(response.statusCode()).isEqualTo(401)

            val inspektør = testRapid.inspektør
            assertThat(inspektør.size).isEqualTo(0)
        }
    }

    private val timeout = Duration.ofSeconds(15)
    private val httpClient = HttpClient.newBuilder().connectTimeout(timeout).build()

    private fun post(url: String, jsonBody: String): HttpResponse<String> =
        httpClient.send(
            HttpRequest.newBuilder(URI(url))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        )

    private fun lagre3KandidaterTilDatabasen(repository: Repository) =
        listOf("111", "222", "333").map {
            val kandidat = Kandidat.fraJson(cvMelding(it))
            repository.lagreKandidat(kandidat)
            kandidat
        }

    private fun lagreNKandidaterTilDatabasen(repository: Repository, n: Int): List<String> {
        var count = 0
        return generateSequence { (count++).takeIf { it < n } }
            .toList()
            .map {
                val kandidat = Kandidat.fraJson(cvMelding(it.toString()))
                repository.lagreKandidat(kandidat)
                kandidat.aktørId
            }
    }
}
