import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.arbeidsgiver.toi.livshendelser.AccessTokenClient
import no.nav.arbeidsgiver.toi.livshendelser.PdlKlient
import no.nav.arbeidsgiver.toi.livshendelser.PersonhendelseService
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.person.pdl.leesah.Endringstype
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.person.pdl.leesah.adressebeskyttelse.Adressebeskyttelse
import no.nav.person.pdl.leesah.adressebeskyttelse.Gradering
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
@Disabled("Vi logger bare foreløpig, publisering til rapid er disablet til vi har verifisert i dev")
class HåndterPersonhendelserTest {

    companion object {
        private val wiremock = WireMockServer(8083).also(WireMockServer::start)
        private val mockOAuth2Server = WireMockServer(18301).also(WireMockServer::start)
        val testRapid = TestRapid()
        val envs = mapOf("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT" to "http://localhost:18301/isso-idtoken/token")
        val personhendelseService =
            PersonhendelseService(testRapid, PdlKlient("http://localhost:8083/graphql", AccessTokenClient(envs)))

        @AfterAll
        fun shutdown() {
            mockOAuth2Server.stop()
            wiremock.stop()
        }
    }

    @BeforeEach
    fun setUp() {
        testRapid.reset()
    }

    @AfterEach
    fun tearDown() {
        wiremock.resetAll()
    }

    @Test
    fun `sjekk at gradering er sendt for en hendelse med en ident`() {

        stubOAtuh()
        stubPdl()

        val personHendelse = personhendelse(
            hendelseId = "id1",
            personidenter = listOf("12312312312"),
            master = "testMaster",
            opprettet = LocalDateTime.of(2023, 1, 1, 0, 0).toInstant(ZoneOffset.UTC),
            opplysningstype = "ADRESSEBESKYTTELSE",
            endringstype = Endringstype.OPPRETTET,
            tidligereHendelseId = "123",
            adressebeskyttelse = Adressebeskyttelse(Gradering.STRENGT_FORTROLIG),
        )

        personhendelseService.håndter(
            listOf(personHendelse)
        )

        val inspektør = testRapid.inspektør
        assertThat(inspektør.size).isEqualTo(1)
        val melding = inspektør.message(0)

        assertThat(melding["@event_name"].asText()).isEqualTo("adressebeskyttelse")
        assertThat(melding["aktørId"].asText()).isEqualTo("987654321")
        assertThat(melding["gradering"].asText()).isEqualTo(Gradering.STRENGT_FORTROLIG.toString())
    }

    @Test
    fun `sjekk at gradering sendes per ident for en person med flere aktørider`() {

        stubOAtuh()
        stubPdl(
            identSvar = """
            {
                "ident" : "987654321"
            },
            {
                "ident" : "987654322"
            }
        """.trimIndent()
        )

        val personHendelse = personhendelse(
            hendelseId = "id1",
            personidenter = listOf("12312312312"),
            master = "testMaster",
            opprettet = LocalDateTime.of(2023, 1, 1, 0, 0).toInstant(ZoneOffset.UTC),
            opplysningstype = "ADRESSEBESKYTTELSE",
            endringstype = Endringstype.OPPRETTET,
            tidligereHendelseId = "123",
            adressebeskyttelse = Adressebeskyttelse(Gradering.STRENGT_FORTROLIG),
        )

        personhendelseService.håndter(
            listOf(personHendelse)
        )

        val inspektør = testRapid.inspektør
        assertThat(inspektør.size).isEqualTo(2)
        val meldinger = listOf(0, 1).map(inspektør::message)

        val keys = listOf(0, 1).map(inspektør::key)
        assertThat(keys).containsExactlyInAnyOrder("987654321", "987654322")

        meldinger.map { assertThat(it["@event_name"].asText()).isEqualTo("adressebeskyttelse") }
        meldinger.map { assertThat(it["gradering"].asText()).isEqualTo(Gradering.STRENGT_FORTROLIG.toString()) }
        assertThat(meldinger.map { it["aktørId"].asText() }).containsExactlyInAnyOrder("987654321", "987654322")
    }

    @Test
    fun `sjekk at gradering håndterer feil`() {

        stubOAtuh()
        stubPdlFeil()

        val personHendelse = personhendelse(
            hendelseId = "id1",
            personidenter = listOf("12312312312"),
            master = "testMaster",
            opprettet = LocalDateTime.of(2023, 1, 1, 0, 0).toInstant(ZoneOffset.UTC),
            opplysningstype = "ADRESSEBESKYTTELSE",
            endringstype = Endringstype.OPPRETTET,
            tidligereHendelseId = "123",
            adressebeskyttelse = Adressebeskyttelse(Gradering.STRENGT_FORTROLIG),
        )


        assertThat(assertThrows<RuntimeException> {
            personhendelseService.håndter(
                listOf(personHendelse)
            )
        }).hasMessage("Noe feil skjedde ved henting av diskresjonskode: ")

        val inspektør = testRapid.inspektør
        assertThat(inspektør.size).isEqualTo(0)
    }

    private fun stubPdl(
        identSvar: String = """
        {
            "ident" : "987654321"
        }
    """.trimIndent()
    ) {
        val pesostegn = "$"
        wiremock.stubFor(
            WireMock.post(WireMock.urlEqualTo("/graphql"))
                .withHeader("Authorization", WireMock.equalTo("Bearer mockedAccessToken"))
                .withRequestBody(
                    WireMock.equalToJson(
                        """
                        {
                            "query": "query( ${pesostegn}ident: ID!) { hentPerson(ident: ${pesostegn}ident, historikk: false) { adressebeskyttelse { gradering }} hentIdenter(ident: ${pesostegn}ident, grupper: [AKTORID], historikk: false) { identer { ident }} }",
                            "variables":{"ident":"12312312312"}
                        }
                    """.trimIndent()
                    )
                )
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withBody(
                            """
                            {
                                "data": {
                                    "hentPerson": {
                                        [
                                            "adressebeskyttelse": {
                                                "gradering" : "STRENGT_FORTROLIG"
                                            }
                                        ]
                                    },
                                    "hentIdenter": {
                                        "identer": [
                                            $identSvar
                                        ]
                                    }
                                }
                            }
                        """.trimIndent()
                        )
                )
        )
    }

    private fun stubPdlFeil() {
        val pesostegn = "$"
        wiremock.stubFor(
            WireMock.post(WireMock.urlEqualTo("/graphql"))
                .withHeader("Authorization", WireMock.equalTo("Bearer mockedAccessToken"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withBody(
                            """
                            {
                              "errors": [ "feil1", "feil2" ]
                            }
                        """.trimIndent()
                        )
                )
        )
    }

    private fun stubOAtuh() {
        val mockedAccessToken = """
            {
                "access_token": "mockedAccessToken",
                "expires_in": 36000
            }
        """.trimIndent()
        mockOAuth2Server.stubFor(
            WireMock.post(WireMock.urlEqualTo("/isso-idtoken/token")).willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withBody(mockedAccessToken)
            )
        )
    }
}

fun personhendelse(
    hendelseId: String,
    personidenter: List<String>,
    master: String,
    opprettet: Instant,
    opplysningstype: String,
    endringstype: Endringstype, // Husk å importere denne klassen
    tidligereHendelseId: String,
    adressebeskyttelse: Adressebeskyttelse // Husk å importere denne klassen
) = Personhendelse(
    hendelseId,
    personidenter,
    master,
    opprettet,
    opplysningstype,
    endringstype,
    tidligereHendelseId,
    adressebeskyttelse
)
