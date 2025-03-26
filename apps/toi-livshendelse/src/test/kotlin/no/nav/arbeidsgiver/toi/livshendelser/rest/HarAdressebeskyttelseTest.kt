package no.nav.arbeidsgiver.toi.livshendelser.rest

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import com.github.tomakehurst.wiremock.WireMockServer
import io.javalin.Javalin
import no.nav.arbeidsgiver.toi.livshendelser.AccessTokenClient
import no.nav.arbeidsgiver.toi.livshendelser.PdlKlient
import no.nav.arbeidsgiver.toi.livshendelser.opprettJavalinMedTilgangskontroll
import no.nav.arbeidsgiver.toi.livshendelser.stubPdl
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.token.support.core.configuration.IssuerProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import java.net.InetAddress
import java.net.URI

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HarAdressebeskyttelseTest {

    private val appPort = 8080
    private val pdlPort = 8083
    private val oAuthPort = 18301
    private val wiremock = WireMockServer(pdlPort).also(WireMockServer::start)
    private val mockOAuth2Server = MockOAuth2Server()


    val envs = mapOf(
        "AZURE_OPENID_CONFIG_TOKEN_ENDPOINT" to "http://localhost:$oAuthPort/isso-idtoken/token",
        "AZURE_APP_CLIENT_SECRET" to "test1",
        "AZURE_APP_CLIENT_ID" to "test2",
        "PDL_SCOPE" to "test3",
    )
    val pdlKlient = PdlKlient("http://localhost:$pdlPort/graphql", AccessTokenClient(envs))
    val testRapid = TestRapid()

    private lateinit var javalin: Javalin

    @BeforeAll
    fun beforeAll() {
        mockOAuth2Server.start(InetAddress.getByName("localhost"), oAuthPort)

    }

    @BeforeEach
    fun beforeEach() {
        javalin = opprettJavalinMedTilgangskontroll(appPort)
    }

    @AfterEach
    fun afterEach() {
        javalin.stop()
    }

    @AfterAll
    fun shutdown() {
        mockOAuth2Server.shutdown()
        wiremock.stop()
    }


    @Test
    fun `Returner at har adressebeskyttelse for person som har adressebeskyttelse`() {
        val fnr = "12345678912"
        wiremock.stubPdl(ident = fnr, token = null)
        val token = hentToken(mockOAuth2Server).serialize()
        startApp(pdlKlient, testRapid)
        val response = Fuel.post("http://localhost:$appPort/adressebeskyttelse")
            .body("""{"fnr": "$fnr"}""")
            .authentication().bearer(token)
            .response().second

        assertThat(response.statusCode).isEqualTo(200)
        assertThat(response.body().asString("application/json; charset=UTF-8")).isEqualTo("""{"harAdressebeskyttelse":true}""")
    }

    @Test
    fun `Returner at person ikke har adressebeskyttelse for person som ikke har adressebeskyttelse`() {
        val fnr = "12345678912"
        wiremock.stubPdl(ident = fnr, gradering = "UGRADERT", token = null)
        val token = hentToken(mockOAuth2Server).serialize()
        startApp(pdlKlient, testRapid)
        val response = Fuel.post("http://localhost:$appPort/adressebeskyttelse")
            .body("""{"fnr": "$fnr"}""")
            .authentication().bearer(token)
            .response().second

        assertThat(response.statusCode).isEqualTo(200)
        assertThat(response.body().asString("application/json; charset=UTF-8")).isEqualTo("""{"harAdressebeskyttelse":false}""")
    }

    @Test
    fun `Feil om token mangler`() {
        val fnr = "12345678912"
        startApp(pdlKlient, testRapid)
        val response = Fuel.post("http://localhost:$appPort/adressebeskyttelse")
            .body("""{"fnr": "$fnr"}""")
            .response().second

        assertThat(response.statusCode).isEqualTo(401)
    }

    @Test
    fun `Feil hvis token er utgått`() {
        val fnr = "12345678912"
        val token = hentToken(mockOAuth2Server, expiry = -1000).serialize()
        startApp(pdlKlient, testRapid)
        val response = Fuel.post("http://localhost:$appPort/adressebeskyttelse")
            .body("""{"fnr": "$fnr"}""")
            .authentication().bearer(token)
            .response().second

        assertThat(response.statusCode).isEqualTo(401)
    }

    @Test
    fun `Sjekk på adressebeskyttelse må ha token med rett issuer`() {
        val fnr = "12345678912"
        val token = hentToken(mockOAuth2Server, issuerId = "falskissuer").serialize()
        startApp(pdlKlient, testRapid)
        val response = Fuel.post("http://localhost:$appPort/adressebeskyttelse")
            .body("""{"fnr": "$fnr"}""")
            .authentication().bearer(token)
            .response().second

        assertThat(response.statusCode).isEqualTo(401)
    }

    @Test
    fun `Sjekk på adressebeskyttelse token må ha Nav-ident`() {
        val fnr = "12345678912"
        val token = hentToken(mockOAuth2Server, navIdent = null).serialize()
        startApp(pdlKlient, testRapid)
        val response = Fuel.post("http://localhost:$appPort/adressebeskyttelse")
            .body("""{"fnr": "$fnr"}""")
            .authentication().bearer(token)
            .response().second

        assertThat(response.statusCode).isEqualTo(401)
    }

    @Test
    fun `Sjekk på adressebeskyttelse må ha token med rett audience`() {
        val fnr = "12345678912"
        val token = hentToken(mockOAuth2Server, audience = listOf("Feil aud")).serialize()
        startApp(pdlKlient, testRapid)
        val response = Fuel.post("http://localhost:$appPort/adressebeskyttelse")
            .body("""{"fnr": "$fnr"}""")
            .authentication().bearer(token)
            .response().second

        assertThat(response.statusCode).isEqualTo(401)
    }

    @Test
    fun `Sjekk på adressebeskyttelse token må ha rett algoritme`() {
        val fnr = "12345678912"
        val payload = hentToken(mockOAuth2Server).serialize().split(".")[1]
        startApp(pdlKlient, testRapid)
        val response = Fuel.post("http://localhost:$appPort/adressebeskyttelse")
            .body("""{"fnr": "$fnr"}""")
            .authentication().bearer("eyJ0eXAiOiJKV1QiLCJhbGciOiJub25lIn0.$payload.")
            .response().second

        assertThat(response.statusCode).isEqualTo(401)
    }

    private fun startApp(
        pdlKlient: PdlKlient,
        rapid: TestRapid
    ) {
        no.nav.arbeidsgiver.toi.livshendelser.startApp(
            rapid, pdlKlient, javalin, mapOf(
                Rolle.VEILEDER to ("isso-idtoken" to IssuerProperties(
                    URI("http://localhost:$oAuthPort/isso-idtoken/.well-known/openid-configuration").toURL(),
                    listOf("audience")
                ))
            )
        )
    }
}

private fun hentToken(
    mockOAuth2Server: MockOAuth2Server,
    issuerId: String = "isso-idtoken",
    navIdent: String? = "A123456",
    audience: List<String> = listOf("audience"),
    expiry: Long = 3600
) = mockOAuth2Server.issueToken(
    issuerId, "someclientid",
    DefaultOAuth2TokenCallback(
        issuerId = issuerId,
        claims = listOfNotNull(
            Pair("name", "navn"),
            navIdent?.let { Pair("NAVident", it) },
            Pair("unique_name", "unique_name"),
        ).toMap(),
        audience = audience,
        expiry = expiry
    )
)