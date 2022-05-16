package no.nav.arbeidsgiver.toi.hullicv

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OrganisasjonsenhetTest {
    private val wireMock = WireMockServer(8082)

    @BeforeAll
    fun setup() {
        wireMock.stubFor(
            get(urlEqualTo("/enhet?enhetsnummerListe=0001")).willReturn(
                aResponse().withBody(svar("0001", "første"))
            )
        )
        wireMock.stubFor(
            get(urlEqualTo("/enhet?enhetsnummerListe=0002")).willReturn(
                aResponse().withBody(svar("0002", "Andre kontor"))
            )
        )
        wireMock.stubFor(
            get(urlEqualTo("/enhet?enhetsnummerListe=0003")).willReturn(
                aResponse().withBody(svar("0003", "The third"))
            )
        )
        wireMock.stubFor(
            get(urlEqualTo("/enhet?enhetsnummerListe=0404")).willReturn(
                aResponse().withStatus(404).withBody(manglendeEnhet("0404"))
            )
        )
        wireMock.stubFor(
            get(urlEqualTo("/enhet?enhetsnummerListe=0405")).willReturn(
                aResponse().withStatus(404)
            )
        )
        wireMock.stubFor(
            get(urlEqualTo("/enhet?enhetsnummerListe=0500")).willReturn(
                aResponse().withStatus(500)
            )
        )
        wireMock.start()
    }

    @AfterAll
    fun tearDown() {
        wireMock.stop()
    }

    @Test
    fun `legg på et eller annet svar om første behov er organisasjonsenhetsnavn`() {
        val testRapid = TestRapid()
        startApp(Norg2Klient("http://localhost:8082"), testRapid)

        testRapid.sendTestMessage(behovsMelding(behovListe = """["organisasjonsenhetsnavn"]"""))

        val inspektør = testRapid.inspektør

        Assertions.assertThat(inspektør.size).isEqualTo(1)
    }

    @Test
    fun `ikke legg på svar om andre behov enn organisasjonsenhetsnavn som ikke er løst`() {
        val testRapid = TestRapid()
        startApp(Norg2Klient("http://localhost:8082"), testRapid)

        testRapid.sendTestMessage(behovsMelding(behovListe = """["noeannet", "organisasjonsenhetsnavn"]"""))

        val inspektør = testRapid.inspektør

        Assertions.assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `legg på svar om behov nummer 2 er organisasjonsenhet, dersom første behov har en løsning`() {
        val testRapid = TestRapid()
        startApp(Norg2Klient("http://localhost:8082"), testRapid)

        testRapid.sendTestMessage(
            behovsMelding(
                behovListe = """["noeannet", "organisasjonsenhetsnavn"]""",
                løsninger = listOf("noeannet" to """{"noeannetsvar": 123}""")
            )
        )

        val inspektør = testRapid.inspektør

        Assertions.assertThat(inspektør.size).isEqualTo(1)
        val melding = inspektør.message(0)
        Assertions.assertThat(melding["organisasjonsenhetsnavn"].asText()).isEqualTo("Andre kontor")
        Assertions.assertThat(melding["noeannet"]["noeannetsvar"].asInt()).isEqualTo(123)
    }

    @Test
    fun `ikke legg på svar om behov er en tom liste`() {
        val testRapid = TestRapid()
        startApp(Norg2Klient("http://localhost:8082"), testRapid)

        testRapid.sendTestMessage(behovsMelding(behovListe = "[]"))

        val inspektør = testRapid.inspektør

        Assertions.assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `ikke legg på svar om svar allerede er lagt på`() {
        val testRapid = TestRapid()
        startApp(Norg2Klient("http://localhost:8082"), testRapid)

        testRapid.sendTestMessage(
            behovsMelding(
                behovListe = """["organisasjonsenhetsnavn"]""",
                løsninger = listOf("organisasjonsenhetsnavn" to """"svar"""")
            )
        )

        val inspektør = testRapid.inspektør

        Assertions.assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `Legg til korrekt NAV-kontor-navn på populert melding`() {
        val testRapid = TestRapid()
        startApp(Norg2Klient("http://localhost:8082"), testRapid)

        testRapid.sendTestMessage(behovsMelding(behovListe = """["organisasjonsenhetsnavn"]"""))

        val inspektør = testRapid.inspektør

        Assertions.assertThat(inspektør.size).isEqualTo(1)

        Assertions.assertThat(inspektør.message(0)["organisasjonsenhetsnavn"].asText()).isEqualTo("Andre kontor")
    }

    @Test
    fun `Om Norg2 returnerer 404 med feilmelding skal vi bruke tom streng`() {
        val testRapid = TestRapid()
        startApp(Norg2Klient("http://localhost:8082"), testRapid)

        testRapid.sendTestMessage(behovsMelding(behovListe = """["organisasjonsenhetsnavn"]""", enhetsNummer = "0404"))

        val inspektør = testRapid.inspektør
        Assertions.assertThat(inspektør.size).isEqualTo(1)
        Assertions.assertThat(inspektør.message(0)["organisasjonsenhetsnavn"].asText()).isEqualTo("")
    }

    @Test
    fun `Om Norg2 returnerer 404 uten feilmelding skal vi stoppe applikasjonen`() {
        val testRapid = TestRapid()
        startApp(Norg2Klient("http://localhost:8082"), testRapid)

        assertThrows<Exception> {
            testRapid.sendTestMessage(behovsMelding(behovListe = """["organisasjonsenhetsnavn"]""", enhetsNummer = "0405"))
        }
    }

    @Test
    fun `Dersom vi får noe annet fra Norg2 utenom 200 og 404 skal vi stoppe applikasjonen`() {
        val testRapid = TestRapid()
        startApp(Norg2Klient("http://localhost:8082"), testRapid)

        assertThrows<Exception> {
            testRapid.sendTestMessage(behovsMelding(behovListe = """["organisasjonsenhetsnavn"]""", enhetsNummer = "0500"))
        }
    }

    private fun behovsMelding(behovListe: String, løsninger: List<Pair<String, String>> = emptyList(), enhetsNummer: String ="0002") = """
        {
            "aktørId":"123",
            "@behov":$behovListe,
            "oppfølgingsinformasjon": {
                "oppfolgingsenhet": "$enhetsNummer"
            }
            ${løsninger.joinToString() { ""","${it.first}":${it.second}""" }}
        }
    """.trimIndent()
}

private fun svar(enhetsnummer: String, navn: String) = """
        [
            {
                "enhetId": 100000050,
                "navn": "$navn",
                "enhetNr": "$enhetsnummer",
                "antallRessurser": 0,
                "status": "Aktiv",
                "orgNivaa": "EN",
                "type": "LOKAL",
                "organisasjonsnummer": "994753924",
                "underEtableringDato": "1970-01-01",
                "aktiveringsdato": "1970-01-01",
                "underAvviklingDato": null,
                "nedleggelsesdato": null,
                "oppgavebehandler": true,
                "versjon": 80,
                "sosialeTjenester": "--> Kontoret har redusert åpningstid pga Korona-viruset\n\nVed akutt behov for nødhjelp, får bruker hjelp i V/P ved oppmøte, ved ordinære saker må bruker avtale time i forkant med veileder, har ikke bruker veileder sender vi Gosys. \n\nOppfølging og timeavtaler vil kunne bli gjennomført pr telefon.\n\nVed behov for akutt nødhjelp mellom 1530 og 0800 kan bruker ta kontakt med kommunenes sosialtjenestevakt (legevakten) på 23487090\n\nBrukere bes om å benytte nav.no eller ringe til NAV/veileder for å avtale tidspunkt for en samtale.\n-------------------------\n\n3 PCer tilgjengelig i V/P som bruker kan benytte til selvbetjening.\n\nNAV Oslo (0300) registrerer Del A for Nav kontorene i Oslo. Spørsmål skal til NAV kontoret.\n\nSosialfaglige henvendelser\nTil saksbehandler, akutte henvendelser settes over til vakttelefoner m/spørreanrop, mellom kl. 9-15 (tlfnr skal ikke oppgis) som følger:\n902 90 073 Nye henvendelser/mottak\n948 32 781 Sosialhjelpsmottakere; Enslige og familier over 29 år\n940 32 093 Ungdomsteamet (Brukere under 30 år)\n\nSpørsmål om frivillig/tvungen forvaltning: tlf 90227384 m/spørreanrop mellom kl 10-11, telefonnummeret kan gis ut til brukere med forvaltning som selv kan ringe mellom 10-11\n\nSosialfaglige tjenester: Gjeldsrådgivning, frivillig/tvungen forvaltning, kommunalt frikort, Nyankomne flyktninger -  Intro program - bosetting av nyankomne flyktninger (bydels kvote)\n\nSender post digitalt.\n\nPapirsøknadsskjema på papir på kommunens nettside og i V/P. \n- Trenger ikke kontakt i forkant for å søke ordinær sosialhjelp - Ved behov for nødhjelp (mat/bolig) kan bruker møte i V/P etter kl 1200 for samtale før søknad\n\nOslo kommune har en døgnåpen vakttjeneste i Storgata 40, de behandler søknader om akutt nødhjelp når NAV-kontoret er stengt.\n\nSaksbehandlingstider: \nØkonomisk sosialhjelp: 14 dager\n\nUtbetaling: \nFast utbetalingsdato: 1 i mnd\nSiste tidspunkt for kjøring: 13:00 \nVed helg/helligdag utbetales det siste virkedag før \nUtbetalingsmåter nødhjelp: Kronekort/kontantkort, Kronekort/kontantkort utleveres i V/P daglig kl 1400\nKvalifiseringsstønad: 28 i mnd.",
                "kanalstrategi": null,
                "orgNrTilKommunaltNavKontor": "974778742"
            }
        ]
    """.trimIndent()

private fun manglendeEnhet(enhetsNummer: String) =
    """{"field":null,"message":"Enheter med gitte parametre eksisterer ikke Enheter: '[$enhetsNummer]' Statuser: 'null' Oppgavebehandler: 'null'"}"""