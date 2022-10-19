package no.nav.arbeidsgiver.toi.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.authentication
import io.javalin.Javalin
import no.nav.arbeidsgiver.toi.*
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.token.support.core.configuration.IssuerProperties
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.*
import java.net.InetAddress
import java.net.URL

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SynlighetTest {
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
    fun `Kall med tom liste skal returnere tomt resultat`() {
        val objectmapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
        val repository = Repository(TestDatabase().dataSource)
        val token = hentToken(mockOAuth2Server)
        val rapid = TestRapid()

        startApp(repository, javalin, rapid) { true }

        rapid.sendTestMessage(Testdata.komplettHendelseSomFørerTilSynlighetTrue())
        Assertions.assertThat(rapid.inspektør.size).isEqualTo(1)

        val kandidater = emptyList<String>()
        val jsonString = objectmapper.writeValueAsString(kandidater)

        val response = Fuel.post("http://localhost:8301/synlighet")
            .authentication().bearer(token.serialize())
            .body(jsonString)
            .response().second

        Assertions.assertThat(response.statusCode).isEqualTo(200)
        Assertions.assertThat(response.body().asString("application/json")).isEqualTo("{}")
    }

    @Test
    fun `Person som er synlig skal returneres som synlig`() {
        val objectmapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
        val repository = Repository(TestDatabase().dataSource)
        val token = hentToken(mockOAuth2Server)
        val rapid = TestRapid()

        startApp(repository, javalin, rapid) { true }

        rapid.sendTestMessage(Testdata.komplettHendelseSomFørerTilSynlighetTrue())
        Assertions.assertThat(rapid.inspektør.size).isEqualTo(1)

        val kandidater = listOf("12345678912")
        val jsonString = objectmapper.writeValueAsString(kandidater)

        val response = Fuel.post("http://localhost:8301/synlighet")
            .authentication().bearer(token.serialize())
            .body(jsonString)
            .response().second

        Assertions.assertThat(response.statusCode).isEqualTo(200)

        val expectedJsonResponse = objectmapper.writeValueAsString(mapOf("12345678912" to true))
        Assertions.assertThat(response.body().asString("application/json")).isEqualTo(expectedJsonResponse)
    }

    @Test
    fun `Personer som er synlige skal returneres som synlige`() {
        val objectmapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
        val repository = Repository(TestDatabase().dataSource)
        val token = hentToken(mockOAuth2Server)
        val rapid = TestRapid()

        startApp(repository, javalin, rapid) { true }

        rapid.sendTestMessage(Testdata.komplettHendelseSomFørerTilSynlighetTrue())
        Assertions.assertThat(rapid.inspektør.size).isEqualTo(1)

        val kandidater = listOf("12345678912", "10000000000")
        val jsonString = objectmapper.writeValueAsString(kandidater)

        val response = Fuel.post("http://localhost:8301/synlighet")
            .authentication().bearer(token.serialize())
            .body(jsonString)
            .response().second

        Assertions.assertThat(response.statusCode).isEqualTo(200)

        val expectedJsonResponse = objectmapper.writeValueAsString(mapOf("12345678912" to true, "10000000000" to false))
        Assertions.assertThat(response.body().asString("application/json")).isEqualTo(expectedJsonResponse)
    }

    @Test
    fun `Person som ikke finnes skal returneres som usynlig`() {
        val objectmapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
        val repository = Repository(TestDatabase().dataSource)
        val token = hentToken(mockOAuth2Server)

        startApp(repository, javalin, TestRapid()) { true }

        val kandidatSomIkkeFinnes = listOf("10000000000")
        val somJson = objectmapper.writeValueAsString(kandidatSomIkkeFinnes)

        val response = Fuel.post("http://localhost:8301/synlighet")
            .authentication().bearer(token.serialize())
            .body(somJson)
            .response().second

        Assertions.assertThat(response.statusCode).isEqualTo(200)

        val expectedJsonResponse = objectmapper.writeValueAsString(mapOf("10000000000" to false))
        Assertions.assertThat(response.body().asString("application/json")).isEqualTo(expectedJsonResponse)
    }

    @Test
    fun `Person som finnes og er usynlig skal returneres som usynlig`() {
        val objectmapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
        val repository = Repository(TestDatabase().dataSource)
        val token = hentToken(mockOAuth2Server)
        val rapid = TestRapid()

        startApp(repository, javalin, rapid) { true }

        rapid.sendTestMessage(Testdata.hendelse(arbeidsmarkedCv = Testdata.arbeidsmarkedCv(meldingstype = CvMeldingstype.OPPRETT)))
        Assertions.assertThat(rapid.inspektør.size).isEqualTo(1)

        val usynligKandidat = listOf("12345678912")
        val jsonString = objectmapper.writeValueAsString(usynligKandidat)

        val response = Fuel.post("http://localhost:8301/synlighet")
            .authentication().bearer(token.serialize())
            .body(jsonString)
            .response().second

        Assertions.assertThat(response.statusCode).isEqualTo(200)

        val expectedJsonResponse = objectmapper.writeValueAsString(mapOf("12345678912" to false))
        Assertions.assertThat(response.body().asString("application/json")).isEqualTo(expectedJsonResponse)
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
}
