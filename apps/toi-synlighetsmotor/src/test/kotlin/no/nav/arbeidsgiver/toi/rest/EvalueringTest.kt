package no.nav.arbeidsgiver.toi.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.authentication
import no.nav.toi.TestRapid
import io.javalin.Javalin
import no.nav.arbeidsgiver.toi.*
import no.nav.arbeidsgiver.toi.Testdata.Companion.komplettHendelseSomFørerTilSynlighetTrue
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.token.support.core.configuration.IssuerProperties
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.*
import java.net.InetAddress
import java.net.URI

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
        javalin = opprettJavalinMedTilgangskontroll()
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
    fun `POST mot evalueringsendepunkt skal returnere 200 OK med evaluering på oppgitt fødselsnummer`() {
        val objectmapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
        val repository = Repository(TestDatabase().dataSource)
        val token = hentToken(mockOAuth2Server)
        val rapid = no.nav.toi.TestRapid()

        startApp(repository, rapid)

        rapid.sendTestMessage(komplettHendelseSomFørerTilSynlighetTrue())
        Assertions.assertThat(rapid.inspektør.size).isEqualTo(1)

        val response = Fuel.post("http://localhost:8301/evaluering")
            .authentication().bearer(token.serialize())
            .body("""{"fnr": "12345678912"}""")
            .response().second

        Assertions.assertThat(response.statusCode).isEqualTo(200)

        val responseJson = response.body().asString("application/json; charset=UTF-8")
        val responeEvaluering = objectmapper.readValue(responseJson, EvalueringUtenDiskresjonskodeDTO::class.java)
        Assertions.assertThat(responeEvaluering).isEqualTo(evalueringUtenDiskresjonskodeMedAltTrue())
    }

    @Test
    fun `Deprekert til fordel for post GET mot evalueringsendepunkt skal returnere 200 OK med evaluering på oppgitt fødselsnummer`() {
        val objectmapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
        val repository = Repository(TestDatabase().dataSource)
        val token = hentToken(mockOAuth2Server)
        val rapid = no.nav.toi.TestRapid()

        startApp(repository, rapid)

        rapid.sendTestMessage(komplettHendelseSomFørerTilSynlighetTrue())
        Assertions.assertThat(rapid.inspektør.size).isEqualTo(1)

        val response = Fuel.get("http://localhost:8301/evaluering/12345678912")
            .authentication().bearer(token.serialize())
            .response().second

        Assertions.assertThat(response.statusCode).isEqualTo(200)

        val responseJson = response.body().asString("application/json; charset=UTF-8")
        val responeEvaluering = objectmapper.readValue(responseJson, EvalueringUtenDiskresjonskodeDTO::class.java)
        Assertions.assertThat(responeEvaluering).isEqualTo(evalueringUtenDiskresjonskodeMedAltTrue())
    }

    @Test // TODO bruk noe annet enn frkas til å flippe status
    fun `POST mot evalueringsendepunkt med oppdatert kandidat skal oppdatere evaluering`() {
        val objectmapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
        val repository = Repository(TestDatabase().dataSource)
        val token = hentToken(mockOAuth2Server)
        val rapid = no.nav.toi.TestRapid()

        startApp(repository, rapid)

        rapid.sendTestMessage(komplettHendelseSomFørerTilSynlighetTrue())
        Assertions.assertThat(rapid.inspektør.size).isEqualTo(1)
        rapid.sendTestMessage(
            komplettHendelseSomFørerTilSynlighetTrue(
                arbeidssøkeropplysninger = Testdata.arbeidssøkeropplysninger(aktiv = true),
            )
        )

        Assertions.assertThat(rapid.inspektør.size).isEqualTo(2)

        val response = Fuel.post("http://localhost:8301/evaluering")
            .authentication().bearer(token.serialize())
            .body("""{"fnr": "12345678912"}""")
            .response().second

        Assertions.assertThat(response.statusCode).isEqualTo(200)

        val responseJson = response.body().asString("application/json; charset=UTF-8")
        val responeEvaluering = objectmapper.readValue(responseJson, EvalueringUtenDiskresjonskodeDTO::class.java)
        Assertions.assertThat(responeEvaluering)
            .isEqualTo(evalueringUtenDiskresjonskodeMedAltTrue().copy(erArbeidssøker = true))
    }

    @Test
    fun `Deprekert til fordel for post GET mot evalueringsendepunkt med oppdatert kandidat skal oppdatere evaluering`() {
        val objectmapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
        val repository = Repository(TestDatabase().dataSource)
        val token = hentToken(mockOAuth2Server)
        val rapid = no.nav.toi.TestRapid()

        startApp(repository, rapid)

        rapid.sendTestMessage(komplettHendelseSomFørerTilSynlighetTrue())
        Assertions.assertThat(rapid.inspektør.size).isEqualTo(1)
        rapid.sendTestMessage(
            komplettHendelseSomFørerTilSynlighetTrue(
                arbeidssøkeropplysninger = Testdata.arbeidssøkeropplysninger(aktiv = true),
            )
        )

        Assertions.assertThat(rapid.inspektør.size).isEqualTo(2)

        val response = Fuel.get("http://localhost:8301/evaluering/12345678912")
            .authentication().bearer(token.serialize())
            .response().second

        Assertions.assertThat(response.statusCode).isEqualTo(200)

        val responseJson = response.body().asString("application/json; charset=UTF-8")
        val responeEvaluering = objectmapper.readValue(responseJson, EvalueringUtenDiskresjonskodeDTO::class.java)
        Assertions.assertThat(responeEvaluering)
            .isEqualTo(evalueringUtenDiskresjonskodeMedAltTrue())
    }

    @Test
    fun `POST mot evalueringsendepunkt skal returnere 200 men med evaluering der alle verdier er false for fødselsnummer som ikke finnes i databasen`() {
        val repository = Repository(TestDatabase().dataSource)
        val rapid = no.nav.toi.TestRapid()
        val token = hentToken(mockOAuth2Server)

        startApp(repository, rapid)

        Assertions.assertThat(rapid.inspektør.size).isEqualTo(0)

        val response = Fuel.post("http://localhost:8301/evaluering")
            .authentication().bearer(token.serialize())
            .body("""{"fnr": "12345678912"}""")
            .response().second

        Assertions.assertThat(response.statusCode).isEqualTo(200)

        val responseJson = response.body().asString("application/json; charset=UTF-8")
        val responeEvaluering =
            jacksonObjectMapper().readValue(responseJson, EvalueringUtenDiskresjonskodeDTO::class.java)
        Assertions.assertThat(responeEvaluering).isEqualTo(evalueringUtenDiskresjonskodeMedAltFalse())
    }

    @Test
    fun `Deprekert til fordel for post GET mot evalueringsendepunkt skal returnere 200 men med evaluering der alle verdier er false for fødselsnummer som ikke finnes i databasen`() {
        val repository = Repository(TestDatabase().dataSource)
        val rapid = no.nav.toi.TestRapid()
        val token = hentToken(mockOAuth2Server)

        startApp(repository, rapid)

        Assertions.assertThat(rapid.inspektør.size).isEqualTo(0)

        val response = Fuel.get("http://localhost:8301/evaluering/12345678912")
            .authentication().bearer(token.serialize())
            .response().second

        Assertions.assertThat(response.statusCode).isEqualTo(200)

        val responseJson = response.body().asString("application/json; charset=UTF-8")
        val responeEvaluering =
            jacksonObjectMapper().readValue(responseJson, EvalueringUtenDiskresjonskodeDTO::class.java)
        Assertions.assertThat(responeEvaluering).isEqualTo(evalueringUtenDiskresjonskodeMedAltFalse())
    }

    private fun startApp(
        repository: Repository,
        rapid: no.nav.toi.TestRapid
    ) {
        no.nav.arbeidsgiver.toi.startApp(repository, javalin, rapid, mapOf(Rolle.VEILEDER to ("isso-idtoken" to IssuerProperties(
            URI("http://localhost:18300/isso-idtoken/.well-known/openid-configuration").toURL(),
            listOf("audience")
        )))) { true }
    }
}

private fun hentToken(mockOAuth2Server: MockOAuth2Server) = mockOAuth2Server.issueToken(
    "isso-idtoken", "someclientid",
    DefaultOAuth2TokenCallback(
        issuerId = "isso-idtoken",
        claims = mapOf(
            Pair("name", "navn"),
            Pair("NAVident", "A123456"),
            Pair("unique_name", "unique_name"),
        ),
        audience = listOf("audience")
    )
)

private fun evalueringUtenDiskresjonskodeMedAltTrue() = EvalueringUtenDiskresjonskodeDTO(
    harAktivCv = true,
    harJobbprofil = true,
    harSettHjemmel = true,
    erUnderOppfoelging = true,
    harRiktigFormidlingsgruppe = true,
    erIkkeSperretAnsatt = true,
    erIkkeDoed = true,
    erArbeidssøker = true,
    erFerdigBeregnet = true
)

private fun evalueringUtenDiskresjonskodeMedAltFalse() = EvalueringUtenDiskresjonskodeDTO(
    harAktivCv = false,
    harJobbprofil = false,
    harSettHjemmel = false,
    erUnderOppfoelging = false,
    harRiktigFormidlingsgruppe = false,
    erIkkeSperretAnsatt = false,
    erIkkeDoed = false,
    erArbeidssøker = false,
    erFerdigBeregnet = false
)