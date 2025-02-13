import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.arbeidsgiver.toi.livshendelser.AccessTokenClient
import no.nav.arbeidsgiver.toi.livshendelser.PdlKlient
import no.nav.arbeidsgiver.toi.livshendelser.PersonhendelseService
import no.nav.person.pdl.leesah.Endringstype
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.person.pdl.leesah.adressebeskyttelse.Adressebeskyttelse
import no.nav.person.pdl.leesah.adressebeskyttelse.Gradering
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.*
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class HåndterPersonhendelserTest {

    companion object {
        private val wiremock = WireMockServer(8083).also(WireMockServer::start)
        private val mockOAuth2Server = WireMockServer(18301).also(WireMockServer::start)
        val envs = mapOf(
            "AZURE_OPENID_CONFIG_TOKEN_ENDPOINT" to "http://localhost:18301/isso-idtoken/token",
            "AZURE_APP_CLIENT_SECRET" to "test1",
            "AZURE_APP_CLIENT_ID" to "test2",
            "PDL_SCOPE" to "test3",
        )
        val pdlKlient = PdlKlient("http://localhost:8083/graphql", AccessTokenClient(envs))
        val testRapid = TestRapid().also { AdressebeskyttelseLytter(pdlKlient, it) }

        val personhendelseService =
            PersonhendelseService(testRapid, pdlKlient)

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
        val aktørId = "123123123"
        stubOAtuh()
        stubPdl(ident = aktørId)

        val personHendelse = personhendelse(personidenter = listOf(aktørId))

        personhendelseService.håndter(
            listOf(personHendelse)
        )

        val inspektør = testRapid.inspektør
        assertThat(inspektør.size).isEqualTo(1)
        val melding = inspektør.message(0)

        assertThat(melding["@event_name"].asText()).isEqualTo("adressebeskyttelse")
        assertThat(melding["aktørId"].asText()).isEqualTo(aktørId)
        assertThat(melding["adressebeskyttelse"].asText()).isEqualTo(Gradering.STRENGT_FORTROLIG.name)
    }

    @Test
    fun `sjekk at hendelser ikke sendes for andre opplysningstype`() {

        stubOAtuh()
        stubPdl()

        val personHendelse = personhendelse(opplysningstype = "NOE_ANNET")

        personhendelseService.håndter(
            listOf(personHendelse)
        )

        val inspektør = testRapid.inspektør
        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `sjekk at en tom liste av hendelser håndteres korrekt`() {

        stubOAtuh()
        stubPdl()

        personhendelseService.håndter(
            listOf()
        )

        val inspektør = testRapid.inspektør
        assertThat(inspektør.size).isEqualTo(0)
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

        val personHendelse = personhendelse()

        personhendelseService.håndter(
            listOf(personHendelse)
        )

        val inspektør = testRapid.inspektør
        assertThat(inspektør.size).isEqualTo(2)
        val meldinger = listOf(0, 1).map(inspektør::message)

        val keys = listOf(0, 1).map(inspektør::key)
        assertThat(keys).containsExactlyInAnyOrder("987654321", "987654322")

        meldinger.map { assertThat(it["@event_name"].asText()).isEqualTo("adressebeskyttelse") }
        meldinger.map { assertThat(it["adressebeskyttelse"].asText()).isEqualTo(Gradering.STRENGT_FORTROLIG.toString()) }
        assertThat(meldinger.map { it["aktørId"].asText() }).containsExactlyInAnyOrder("987654321", "987654322")
    }

    @Test
    fun `sjekk at gradering håndterer feil`() {

        stubOAtuh()
        stubPdlFeil()

        val personHendelse = personhendelse()


        assertThat(assertThrows<RuntimeException> {
            personhendelseService.håndter(
                listOf(personHendelse)
            )
        }).hasMessage("Noe feil skjedde ved henting av diskresjonskode: ")

        val inspektør = testRapid.inspektør
        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `legg på svar om første behov er adressebeskyttelse`() {
        val aktørId = "123123123"
        stubOAtuh()
        stubPdl(ident = aktørId)

        testRapid.sendTestMessage(behovsMelding(ident = aktørId, behovListe = """["adressebeskyttelse"]"""))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
        val message = inspektør.message(0)
        assertThat(message["adressebeskyttelse"].asText()).isEqualTo("CHECK_DISABLED")
    }

    @Test
    fun `ikke legg på svar om andre uløste behov enn adressebeskyttelse er først i listen`() {
        val aktørId = "123123123"
        stubOAtuh()
        stubPdl(ident = aktørId)

        testRapid.sendTestMessage(behovsMelding(ident = aktørId, behovListe = """["noeannet", "adressebeskyttelse"]"""))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `legg på svar om behov nummer 2 er adressebeskyttelse, dersom første behov har en løsning`() {
        val aktørId = "123123123"
        stubOAtuh()
        stubPdl(ident = aktørId)

        testRapid.sendTestMessage(
            behovsMelding(
                ident = aktørId,
                behovListe = """["noeannet", "adressebeskyttelse"]""",
                løsninger = listOf("noeannet" to """{"noeannetsvar": 123}""")
            )
        )

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
        val melding = inspektør.message(0)
        assertThat(melding["adressebeskyttelse"].asText()).isEqualTo("CHECK_DISABLED")
        assertThat(melding["noeannet"]["noeannetsvar"].asInt()).isEqualTo(123)
    }

    @Test
    fun `ikke legg på svar om behov er en tom liste`() {
        testRapid.sendTestMessage(behovsMelding(behovListe = "[]"))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `ikke legg på svar om svar allerede er lagt på`() {
        testRapid.sendTestMessage(
            behovsMelding(
                behovListe = """["adressebeskyttelse"]""",
                løsninger = listOf("adressebeskyttelse" to """"svar"""")
            )
        )

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `legg på svar om behov nummer 2 er adressebeskyttelse, dersom første behov har en løsning med null-verdi`() {
        val aktørId = "123123123"
        stubOAtuh()
        stubPdl(ident = aktørId)

        testRapid.sendTestMessage(
            behovsMelding(
                ident = aktørId,
                behovListe = """["noeannet", "adressebeskyttelse"]""",
                løsninger = listOf("noeannet" to "null")
            )
        )

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
        val melding = inspektør.message(0)
        assertThat(melding["adressebeskyttelse"].asText()).isEqualTo("CHECK_DISABLED")
        assertThat(melding["noeannet"].isNull).isTrue
    }

    @Test
    fun `ikke legg på svar om svar allerede er lagt på med null-verdi`() {
        testRapid.sendTestMessage(
            behovsMelding(
                behovListe = """["adressebeskyttelse"]""",
                løsninger = listOf("adressebeskyttelse" to "null")
            )
        )

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `sjekk at hentGraderingPerAktørId returnerer UKJENT ved 'Fant ikke person' error`() {
        val ident = "111111111"
        stubOAtuh()
        stubPdlPersonNotFound(ident)
        val resultat = pdlKlient.hentGraderingPerAktørId(ident)
        assertThat(resultat).containsEntry(ident, "UKJENT")
    }

    @Test
    fun `sjekk at hentGraderingPerAktørId kaster exception ved annen error`() {
        val ident = "222222222"
        stubOAtuh()
        stubPdlOtherError(ident)
        assertThatThrownBy { pdlKlient.hentGraderingPerAktørId(ident) }
            .hasMessageContaining("Klarte ikke å hente gradering fra PDL-respons")
    }

    private fun stubPdlPersonNotFound(ident: String = "12312312312") {
        val pesostegn = "$"
        wiremock.stubFor(
            WireMock.post(WireMock.urlEqualTo("/graphql"))
                .withHeader("Authorization", WireMock.equalTo("Bearer mockedAccessToken"))
                .withRequestBody(
                    WireMock.equalToJson(
                        """
                    {
                        "query": "query( ${pesostegn}ident: ID!) { hentPerson(ident: ${pesostegn}ident) { adressebeskyttelse(historikk: false) { gradering }} hentIdenter(ident: ${pesostegn}ident, grupper: [AKTORID], historikk: false) { identer { ident }} }",
                        "variables":{"ident":"$ident"}
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
                            "errors": [ { "message": "Fant ikke person" } ]
                        }
                        """.trimIndent()
                        )
                )
        )
    }

    private fun stubPdlOtherError(ident: String = "12312312312") {
        val pesostegn = "$"
        wiremock.stubFor(
            WireMock.post(WireMock.urlEqualTo("/graphql"))
                .withHeader("Authorization", WireMock.equalTo("Bearer mockedAccessToken"))
                .withRequestBody(
                    WireMock.equalToJson(
                        """
                    {
                        "query": "query( ${pesostegn}ident: ID!) { hentPerson(ident: ${pesostegn}ident) { adressebeskyttelse(historikk: false) { gradering }} hentIdenter(ident: ${pesostegn}ident, grupper: [AKTORID], historikk: false) { identer { ident }} }",
                        "variables":{"ident":"$ident"}
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
                            "errors": [ { "message": "Noe annet error" } ]
                        }
                        """.trimIndent()
                        )
                )
        )
    }

    private fun stubPdl(
        ident: String = "12312312312",
        identSvar: String = """
        {
            "ident" : "$ident"
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
                            "query": "query( ${pesostegn}ident: ID!) { hentPerson(ident: ${pesostegn}ident) { adressebeskyttelse(historikk: false) { gradering }} hentIdenter(ident: ${pesostegn}ident, grupper: [AKTORID], historikk: false) { identer { ident }} }",
                            "variables":{"ident":"$ident"}
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
                                            "adressebeskyttelse": [
                                                {
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
    hendelseId: String = "id1",
    personidenter: List<String> = listOf("12312312312"),
    master: String = "testMaster",
    opprettet: Instant = LocalDateTime.of(2023, 1, 1, 0, 0).toInstant(ZoneOffset.UTC),
    opplysningstype: String = "ADRESSEBESKYTTELSE_V1",
    endringstype: Endringstype = Endringstype.OPPRETTET,
    tidligereHendelseId: String = "123",
    adressebeskyttelse: Adressebeskyttelse = Adressebeskyttelse(Gradering.STRENGT_FORTROLIG)
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

private fun behovsMelding(
    ident: String = "12312312312",
    behovListe: String,
    løsninger: List<Pair<String, String>> = emptyList(),
) = """
        {
            "aktørId":"$ident",
            "@behov":$behovListe
            ${løsninger.joinToString() { ""","${it.first}":${it.second}""" }}
        }
    """.trimIndent()
