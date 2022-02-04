package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.authentication
import io.javalin.Javalin
import no.nav.arbeidsgiver.toi.Testdata.Companion.komplettHendelseSomFørerTilSynlighetTrue
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.token.support.core.configuration.IssuerProperties
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.*
import java.net.InetAddress
import java.net.URL

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EvalueringTest {
    private val mockOAuth2Server = MockOAuth2Server()
    private lateinit var javalin: Javalin

    @BeforeAll
    fun beforeAll() {
        mockOAuth2Server.start(InetAddress.getByName("localhost"), 18300)
    }

    @BeforeEach
    fun beforeEach() {
        val issuerProperties = IssuerProperties(
            URL("http://localhost:18300/isso-idtoken/.well-known/openid-configuration"),
            listOf("audience"),
            "isso-idtoken"
        )

        javalin = opprettJavalinMedTilgangskontroll(listOf(issuerProperties))
    }

    @AfterEach
    fun afterEach() {
        javalin.stop()
    }

    @AfterAll
    fun afterAll() {
        mockOAuth2Server.shutdown()
    }

   @Test
    fun `Synlighetsevaluering som følge av melding skal lagres på personen i databasen`() {
        val repository = Repository(TestDatabase().dataSource)

        testProgramMedHendelse(
            komplettHendelseSomFørerTilSynlighetTrue(),
            enHendelseErPublisertMedSynlighetsverdiOgFerdigBeregnet(
                synlighet = true,
                ferdigBeregnet = true
            ),
            repository
        )

        val evalueringFraDb = repository.hentMedAktørid(aktorId = "123456789")
        Assertions.assertThat(evalueringFraDb).isEqualTo(
            evalueringMedAltTrue()
        )
    }

    @Test
    fun `GET mot evalueringsendepunkt skal returnere 200 OK med evaluering på oppgitt fødselsnummer`() {
        val objectmapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
        val repository = Repository(TestDatabase().dataSource)
        val token = hentToken(mockOAuth2Server)
        val rapid = TestRapid()

        startApp(repository, javalin, rapid)

        rapid.sendTestMessage(komplettHendelseSomFørerTilSynlighetTrue())
        Assertions.assertThat(rapid.inspektør.size).isEqualTo(1)

        val response = Fuel.get("http://localhost:9000/evaluering/12345678912")
            .authentication().bearer(token.serialize())
            .response().second

        Assertions.assertThat(response.statusCode).isEqualTo(200)

        val responseJson = response.body().asString("application/json")
        val responeEvaluering = objectmapper.readValue(responseJson, Evaluering::class.java)
        Assertions.assertThat(responeEvaluering).isEqualTo(evalueringMedAltTrue())
    }

    @Test
    fun `GET mot evalueringsendepunkt med oppdatert kandidat skal oppdatere evaluering`() {
        val objectmapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
        val repository = Repository(TestDatabase().dataSource)
        val token = hentToken(mockOAuth2Server)
        val rapid = TestRapid()

        startApp(repository, javalin, rapid)

        rapid.sendTestMessage(komplettHendelseSomFørerTilSynlighetTrue())
        Assertions.assertThat(rapid.inspektør.size).isEqualTo(1)
        rapid.sendTestMessage(komplettHendelseSomFørerTilSynlighetTrue(fritattKandidatsøk = """
            "fritattKandidatsøk" : {
                "fritattKandidatsok" : true
            }
        """))

        Assertions.assertThat(rapid.inspektør.size).isEqualTo(2)

        val response = Fuel.get("http://localhost:9000/evaluering/12345678912")
            .authentication().bearer(token.serialize())
            .response().second

        Assertions.assertThat(response.statusCode).isEqualTo(200)

        val responseJson = response.body().asString("application/json")
        val responeEvaluering = objectmapper.readValue(responseJson, Evaluering::class.java)
        Assertions.assertThat(responeEvaluering).isEqualTo(evalueringMedAltTrue().copy(erIkkefritattKandidatsøk = false))
    }

    @Test
    fun `GET mot evalueringsendepunkt skal returnere 404 for fødselsnummer som ikke finnes i databasen`() {
        val repository = Repository(TestDatabase().dataSource)
        val rapid = TestRapid()
        val token = hentToken(mockOAuth2Server)

        startApp(repository, javalin, rapid)

        Assertions.assertThat(rapid.inspektør.size).isEqualTo(0)

        val response = Fuel.get("http://localhost:9000/evaluering/12345678912")
            .authentication().bearer(token.serialize())
            .response().second

        Assertions.assertThat(response.statusCode).isEqualTo(404)
    }
}

private fun hentToken(mockOAuth2Server: MockOAuth2Server) = mockOAuth2Server.issueToken("isso-idtoken", "someclientid",
    DefaultOAuth2TokenCallback(
        issuerId = "isso-idtoken",
        claims = mapOf(
            Pair("name", "navn"),
            Pair("NAVident", "NAVident"),
            Pair("unique_name", "unique_name"),
        ),
        audience = listOf("audience")
    )
)

private fun evalueringMedAltTrue() = Evaluering(
    harAktivCv = true,
    harJobbprofil = true,
    harSettHjemmel = true,
    maaIkkeBehandleTidligereCv = true,
    erIkkefritattKandidatsøk = true,
    erUnderOppfoelging = true,
    harRiktigFormidlingsgruppe = true,
    erIkkeKode6eller7 = true,
    erIkkeSperretAnsatt = true,
    erIkkeDoed = true,
    erFerdigBeregnet = true
)
