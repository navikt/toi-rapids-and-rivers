package no.nav.arbeidsgiver.toi.livshendelser.rest

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import com.github.tomakehurst.wiremock.WireMockServer
import io.javalin.Javalin
import no.nav.arbeidsgiver.toi.*
import no.nav.arbeidsgiver.toi.livshendelser.AccessTokenClient
import no.nav.arbeidsgiver.toi.livshendelser.PdlKlient
import no.nav.arbeidsgiver.toi.livshendelser.opprettJavalinMedTilgangskontroll
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.token.support.core.configuration.IssuerProperties
import org.junit.jupiter.api.*
import java.net.InetAddress
import java.net.URI

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HentAdressebeskyttelseTest {

    private val wiremock = WireMockServer(8083).also(WireMockServer::start)
    private val mockOAuth2Server = MockOAuth2Server()


    val envs = mapOf(
        "AZURE_OPENID_CONFIG_TOKEN_ENDPOINT" to "http://localhost:18301/isso-idtoken/token",
        "AZURE_APP_CLIENT_SECRET" to "test1",
        "AZURE_APP_CLIENT_ID" to "test2",
        "PDL_SCOPE" to "test3",
    )
    val pdlKlient = PdlKlient("http://localhost:8083/graphql", AccessTokenClient(envs))
    val testRapid = TestRapid()

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
    fun shutdown() {
        mockOAuth2Server.shutdown()
        wiremock.stop()
    }


    @Test
    fun `Hent adressebeskyttelse for person som har adressebeskyttelse`() {
        val token = hentToken(mockOAuth2Server)
        startApp(pdlKlient, testRapid)
        val response = Fuel.get("http://localhost:8301/adressebeskyttelse")
            .authentication().bearer(token.serialize())
            .body("""{"fnr": "12345678912"}""")
            .response().second
    }

    private fun startApp(
        pdlKlient: PdlKlient,
        rapid: TestRapid
    ) {
        no.nav.arbeidsgiver.toi.livshendelser.startApp(
            rapid, pdlKlient, javalin, mapOf(
                Rolle.VEILEDER to ("isso-idtoken" to IssuerProperties(
                    URI("http://localhost:18300/isso-idtoken/.well-known/openid-configuration").toURL(),
                    listOf("audience")
                ))
            )
        )
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