package no.nav.arbeidsgiver.toi.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.authentication
import io.javalin.Javalin
import no.nav.arbeidsgiver.toi.*
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

        javalin = opprettJavalinMedTilgangskontroll(
            mapOf(
                Rolle.VEILEDER to issuerProperties,
                Rolle.ARBEIDSGIVER to issuerProperties
            )
        )
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
    fun `GET mot evalueringsendepunkt skal returnere 200 OK med evaluering på oppgitt fødselsnummer`() {
        val objectmapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
        val repository = Repository(TestDatabase().dataSource)
        val token = hentToken(mockOAuth2Server)
        val rapid = TestRapid()

        startApp(repository, javalin, rapid)

        rapid.sendTestMessage(komplettHendelseSomFørerTilSynlighetTrue())
        Assertions.assertThat(rapid.inspektør.size).isEqualTo(1)

        val response = Fuel.get("http://localhost:8301/evaluering/12345678912")
            .authentication().bearer(token.serialize())
            .response().second

        Assertions.assertThat(response.statusCode).isEqualTo(200)

        val responseJson = response.body().asString("application/json")
        val responeEvaluering = objectmapper.readValue(responseJson, EvalueringUtenDiskresjonskodeDTO::class.java)
        Assertions.assertThat(responeEvaluering).isEqualTo(evalueringUtenDiskresjonskodeMedAltTrue())
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
        rapid.sendTestMessage(
            komplettHendelseSomFørerTilSynlighetTrue(
                fritattKandidatsøk = """
            "fritattKandidatsøk" : {
                "fritattKandidatsok" : true
            }
        """
            )
        )

        Assertions.assertThat(rapid.inspektør.size).isEqualTo(2)

        val response = Fuel.get("http://localhost:8301/evaluering/12345678912")
            .authentication().bearer(token.serialize())
            .response().second

        Assertions.assertThat(response.statusCode).isEqualTo(200)

        val responseJson = response.body().asString("application/json")
        val responeEvaluering = objectmapper.readValue(responseJson, EvalueringUtenDiskresjonskodeDTO::class.java)
        Assertions.assertThat(responeEvaluering)
            .isEqualTo(evalueringUtenDiskresjonskodeMedAltTrue().copy(erIkkeFritattKandidatsøk = false))
    }

    @Test
    fun `GET mot evalueringsendepunkt skal returnere 200 men med evaluering der alle verdier er false for fødselsnummer som ikke finnes i databasen`() {
        val repository = Repository(TestDatabase().dataSource)
        val rapid = TestRapid()
        val token = hentToken(mockOAuth2Server)

        startApp(repository, javalin, rapid)

        Assertions.assertThat(rapid.inspektør.size).isEqualTo(0)

        val response = Fuel.get("http://localhost:8301/evaluering/12345678912")
            .authentication().bearer(token.serialize())
            .response().second

        Assertions.assertThat(response.statusCode).isEqualTo(200)

        val responseJson = response.body().asString("application/json")
        val responeEvaluering =
            jacksonObjectMapper().readValue(responseJson, EvalueringUtenDiskresjonskodeDTO::class.java)
        Assertions.assertThat(responeEvaluering).isEqualTo(evalueringUtenDiskresjonskodeMedAltFalse())
    }
}

private fun hentToken(mockOAuth2Server: MockOAuth2Server) = mockOAuth2Server.issueToken(
    "isso-idtoken", "someclientid",
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

private fun evalueringUtenDiskresjonskodeMedAltTrue() = EvalueringUtenDiskresjonskodeDTO(
    harAktivCv = true,
    harJobbprofil = true,
    harSettHjemmel = true,
    maaIkkeBehandleTidligereCv = true,
    erIkkeFritattKandidatsøk = true,
    erUnderOppfoelging = true,
    harRiktigFormidlingsgruppe = true,
    erIkkeSperretAnsatt = true,
    erIkkeDoed = true,
    erFerdigBeregnet = true
)

private fun evalueringUtenDiskresjonskodeMedAltFalse() = EvalueringUtenDiskresjonskodeDTO(
    harAktivCv = false,
    harJobbprofil = false,
    harSettHjemmel = false,
    maaIkkeBehandleTidligereCv = false,
    erIkkeFritattKandidatsøk = false,
    erUnderOppfoelging = false,
    harRiktigFormidlingsgruppe = false,
    erIkkeSperretAnsatt = false,
    erIkkeDoed = false,
    erFerdigBeregnet = false
)