package no.nav.arbeidsgiver.toi.arbeidsgiver.notifikasjon

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NotifikasjonKlientLytterTest {

    private val testRapid = TestRapid()
    private val urlNotifikasjonApi = "http://localhost:8082/api/graphql"
    private val pesostegn = "$"
    private val accessToken = "TestAccessToken"

    private val notifikasjonKlient = NotifikasjonKlient(urlNotifikasjonApi) { accessToken }
    private val notifikasjonLytter = NotifikasjonLytter(testRapid, notifikasjonKlient)

    private val wiremock = WireMockServer(8082).also { it.start() }

    @BeforeEach
    fun beforeEach() {
        wiremock.resetAll()
        testRapid.reset()
    }

    @Test
    fun `Når vi mottar notifikasjonsmelding på rapid skal vi gjøre kall til notifikasjonssystemet`() {
        val melding = """
            {
              "@event_name": "notifikasjon.cv-delt",
              "notifikasjonsId": "enEllerAnnenId",
              "arbeidsgiversEpostadresser": ["test@testepost.no"], 
              "stillingsId": "666028e2-d031-4d53-8a44-156efc1a3385",
              "virksomhetsnummer": "123456789",
              "utførtAvVeilederFornavn": "Veileder",
              "utførtAvVeilederEtternavn": "Veiledersen",
              "tidspunktForHendelse": "2023-02-09T10:37:45.108+01:00",
              "meldingTilArbeidsgiver": "Her\nhar du noen \n fine kandidater med \"hermetegn\".",
              "stillingstittel": "En fantastisk stilling!"
            }
        """.trimIndent()
        stubKallTilNotifikasjonssystemet()

        testRapid.sendTestMessage(melding)

        wiremock.verify(
            1, postRequestedFor(
                urlEqualTo("/api/graphql")
            ).withRequestBody(
                equalTo(
                    " " +
                            """
                      { "query": "mutation OpprettNyBeskjed( ${pesostegn}eksternId: String! ${pesostegn}grupperingsId: String! ${pesostegn}merkelapp: String! ${pesostegn}virksomhetsnummer: String! ${pesostegn}epostTittel: String! ${pesostegn}epostBody: String! ${pesostegn}lenke: String! ${pesostegn}tidspunkt: ISO8601DateTime! ${pesostegn}hardDeleteDuration: ISO8601Duration! ${pesostegn}notifikasjonTekst: String! ${pesostegn}epostadresse1: String! ) { nyBeskjed ( nyBeskjed: { metadata: { virksomhetsnummer: ${pesostegn}virksomhetsnummer eksternId: ${pesostegn}eksternId opprettetTidspunkt: ${pesostegn}tidspunkt grupperingsid: ${pesostegn}grupperingsId hardDelete: { om: ${pesostegn}hardDeleteDuration } } mottaker: { altinn: { serviceEdition: \"1\" serviceCode: \"5078\" } } notifikasjon: { merkelapp: ${pesostegn}merkelapp tekst: ${pesostegn}notifikasjonTekst lenke: ${pesostegn}lenke } eksterneVarsler: [ { epost: { epostTittel: ${pesostegn}epostTittel epostHtmlBody: ${pesostegn}epostBody mottaker: { kontaktinfo: { epostadresse: ${pesostegn}epostadresse1 } } sendetidspunkt: { sendevindu: LOEPENDE } } } ] } ) { __typename ... on NyBeskjedVellykket { id } ... on Error { feilmelding } } }", "variables": { "epostadresse1": "test@testepost.no", "eksternId": "enEllerAnnenId", "grupperingsId": "666028e2-d031-4d53-8a44-156efc1a3385", "merkelapp": "Kandidater", "virksomhetsnummer": "123456789", "epostTittel": "Kandidater fra NAV", "epostBody": "<html> <head> <meta http-equiv='Content-Type' content='text/html; charset=UTF-8'/> <title>En fantastisk stilling!</title> </head> <body style='font-family: sans-serif; padding:40px 20px; color: #262626'> <div> <h1 style='font-size: 1.75rem; font-weight:bold;'>Hei.</h1> <h2 style='font-weight: 500; font-size: 1rem;'>Vi har funnet nye kandidater for deg til stillingen: <b>En fantastisk stilling!</b>.</h2> <pre style='font-family: unset;'>Her<br/>har du noen <br/> fine kandidater med \"hermetegn\".</pre> </div> <div style='padding-block-start:32px;'> <h3 style='font-size: 1rem; line-height: 1rem; font-weight:bold;'>Se kandidatene dine</h3> <div style='font-size: 1rem; padding: 12px 16px; border-radius: 12px; background-color:#F7F7F7;'> <p> <ol style='margin-block: 0px; padding-inline-start: 24px;'> <li style='margin-block-end: 0.5rem;'>Åpne <a href='#' style='text-decoration:none; color:#000; cursor: default; font-weight: bold;'>https://arbeidsgiver.nav.no/min-side-arbeidsgiver</a></li> <li style='margin-block-end: 0.5rem;'><b>Logg inn</b></li> <li>Finn <b>varslene dine</b> oppe til høyre på siden, og trykk deg inn på meldingen, eller finn lenken i kortet med teksten <b>Kandidater til mine stillinger</b> lenger ned på siden.</li> </ol> </p> </div> </div> <p style='padding-block:40px 32px'>Vennlig hilsen Veileder Veiledersen</p> <div style='border:1px solid rgba(0, 0, 0, 0.1)'></div> <h2 style='font-size: 1rem; line-height: 1rem; font-weight:bold; margin-block: 56px 20px; color:rgba(0,0,0,0.56);'>Mangler du tilgang til Min Side for Arbeidsgiver hos NAV?</h2> <div style='font-size: 1rem; padding:24px 24px; border-radius: 12px; background-color:#F7F7F7;'> <p style='padding-block: 0px; margin-block-end: 0.5rem; margin-block-start: 0px'>Tilgangen til NAVs rekrutteringstjenester styrer arbeidsgivere selv i <b>Altinn</b>.</p> <p style='margin-block-end: 0.5rem;'>For å få tilgang må du kontakte den som styrer tilgangene til virksomheten din. Det kan være noen i HR, en leder, mellomleder, eller noen på eiersiden i virksomheten.</p> <p style='margin-block-end: 0.5rem;'>Vi har lagd en oppskrift du kan dele med vedkommende for å gjøre det enklere for dem å gi deg tilgang.</p> <h3 style='font-size: 1rem;line-height: 1rem; font-weight:500; margin-block: 40px 32px'>Kopier den gjerne og send den til vedkommende:</h3> <div style='font-size: 1rem; padding:24px 24px; border-radius: 12px; border: 3px dashed rgba(0, 0, 0, 0.1); background-color:#fff;'> <p style='padding-block: 0px; margin-block-end: 0.5rem; margin-block-start: 0px'>Du får denne meldingen fordi avsender ønsker å få tilgang til CV-er fra NAV på vegne av virksomheten din. </p> <b>Gi tilganger til CV-er fra NAV:</b> <ol> <li>Logg inn i Altinn</li> <li>Velg virksomheten din under 'Alle dine aktører'</li> <li>Trykk på knappen 'Profil' øverst i menyen</li> <li>Trykk på 'Andre med rettigheter til virksomheten'</li> <li>Velg 'Legge til ny person eller virksomhet'</li> <li>Legg inn personnummeret og etternavnet til personen som skal ha tilgang</li> <li>Velg 'Gi tilgang til enkelttjenester'</li> <li>Skriv 'Rekruttering', så vil alternativet komme opp som et valg. Velg 'Rekruttering'.</li> <li>Bekreft</li> </ol> <p> Denne enkeltrettigheten gir kun tilgang til å bruke NAV sine rekrutteringstjenester: publisere stillingsannonser og holde videomøter for stillinger på Arbeidsplassen, og motta CV-er fra NAV på <a href='#' style='text-decoration:none; color:#000; cursor: default; font-weight: bold;'>https://arbeidsgiver.nav.no/min-side-arbeidsgiver</a>. </p> <b>Ga ikke Altinn deg muligheten til å gi tilgang?</b> <p>Du kan gi tilgang hvis du har en av disse rollene:</p> <ul> <li>Du er registrert i Enhetsregisteret som daglig leder, styrets leder, bestyrende reder eller innehaver.</li> <li>Du er registert som hovedadministrator i Altinn.</li> <li>Du er 'Tilgangsstyrer' i Altinn og har én, eller flere av rollene: 'Rekruttering', 'Lønn og personalmedarbeider', eller 'Utfyller/innsender'.</li> </ul> </div> </div> </body></html>", "lenke": "https://presenterte-kandidater.intern.dev.nav.no/kandidatliste/666028e2-d031-4d53-8a44-156efc1a3385?virksomhet=123456789", "tidspunkt": "2023-02-09T10:37:45+01:00", "hardDeleteDuration": "P3M", "notifikasjonTekst": "Din virksomhet har mottatt nye kandidater" } }
                    """.trimIndent()
                )
            )
        )
        assertThat(testRapid.inspektør.size).isZero
    }

    @Test
    fun `Når vi har flere emailer, skal vi sende flere emailadresser til notfikasjonssystemet`() {
        val melding = """
            {
              "@event_name": "notifikasjon.cv-delt",
              "notifikasjonsId": "enHeltAnnenId",
              "arbeidsgiversEpostadresser": ["test@testepost.no","test2@testepost.no"], 
              "stillingsId": "666028e2-d031-4d53-8a44-156efc1a3385",
              "virksomhetsnummer": "123456789",
              "utførtAvVeilederFornavn": "Veileder",
              "utførtAvVeilederEtternavn": "Veiledersen",
              "tidspunktForHendelse": "2023-02-09T10:37:45.108+01:00",
              "meldingTilArbeidsgiver": "Her har du noen fine kandidater!",
              "stillingstittel": "En fantastisk stilling!"
            }
        """.trimIndent()
        stubKallTilNotifikasjonssystemet()

        testRapid.sendTestMessage(melding)

        wiremock.verify(
            1, postRequestedFor(
                urlEqualTo("/api/graphql")
            ).withRequestBody(
                equalTo(
                    " " +
                            """
                        { "query": "mutation OpprettNyBeskjed( ${pesostegn}eksternId: String! ${pesostegn}grupperingsId: String! ${pesostegn}merkelapp: String! ${pesostegn}virksomhetsnummer: String! ${pesostegn}epostTittel: String! ${pesostegn}epostBody: String! ${pesostegn}lenke: String! ${pesostegn}tidspunkt: ISO8601DateTime! ${pesostegn}hardDeleteDuration: ISO8601Duration! ${pesostegn}notifikasjonTekst: String! ${pesostegn}epostadresse1: String! ${pesostegn}epostadresse2: String! ) { nyBeskjed ( nyBeskjed: { metadata: { virksomhetsnummer: ${pesostegn}virksomhetsnummer eksternId: ${pesostegn}eksternId opprettetTidspunkt: ${pesostegn}tidspunkt grupperingsid: ${pesostegn}grupperingsId hardDelete: { om: ${pesostegn}hardDeleteDuration } } mottaker: { altinn: { serviceEdition: \"1\" serviceCode: \"5078\" } } notifikasjon: { merkelapp: ${pesostegn}merkelapp tekst: ${pesostegn}notifikasjonTekst lenke: ${pesostegn}lenke } eksterneVarsler: [ { epost: { epostTittel: ${pesostegn}epostTittel epostHtmlBody: ${pesostegn}epostBody mottaker: { kontaktinfo: { epostadresse: ${pesostegn}epostadresse1 } } sendetidspunkt: { sendevindu: LOEPENDE } } }, { epost: { epostTittel: ${pesostegn}epostTittel epostHtmlBody: ${pesostegn}epostBody mottaker: { kontaktinfo: { epostadresse: ${pesostegn}epostadresse2 } } sendetidspunkt: { sendevindu: LOEPENDE } } } ] } ) { __typename ... on NyBeskjedVellykket { id } ... on Error { feilmelding } } }", "variables": { "epostadresse1": "test@testepost.no", "epostadresse2": "test2@testepost.no", "eksternId": "enHeltAnnenId", "grupperingsId": "666028e2-d031-4d53-8a44-156efc1a3385", "merkelapp": "Kandidater", "virksomhetsnummer": "123456789", "epostTittel": "Kandidater fra NAV", "epostBody": "<html> <head> <meta http-equiv='Content-Type' content='text/html; charset=UTF-8'/> <title>En fantastisk stilling!</title> </head> <body style='font-family: sans-serif; padding:40px 20px; color: #262626'> <div> <h1 style='font-size: 1.75rem; font-weight:bold;'>Hei.</h1> <h2 style='font-weight: 500; font-size: 1rem;'>Vi har funnet nye kandidater for deg til stillingen: <b>En fantastisk stilling!</b>.</h2> <pre style='font-family: unset;'>Her har du noen fine kandidater!</pre> </div> <div style='padding-block-start:32px;'> <h3 style='font-size: 1rem; line-height: 1rem; font-weight:bold;'>Se kandidatene dine</h3> <div style='font-size: 1rem; padding: 12px 16px; border-radius: 12px; background-color:#F7F7F7;'> <p> <ol style='margin-block: 0px; padding-inline-start: 24px;'> <li style='margin-block-end: 0.5rem;'>Åpne <a href='#' style='text-decoration:none; color:#000; cursor: default; font-weight: bold;'>https://arbeidsgiver.nav.no/min-side-arbeidsgiver</a></li> <li style='margin-block-end: 0.5rem;'><b>Logg inn</b></li> <li>Finn <b>varslene dine</b> oppe til høyre på siden, og trykk deg inn på meldingen, eller finn lenken i kortet med teksten <b>Kandidater til mine stillinger</b> lenger ned på siden.</li> </ol> </p> </div> </div> <p style='padding-block:40px 32px'>Vennlig hilsen Veileder Veiledersen</p> <div style='border:1px solid rgba(0, 0, 0, 0.1)'></div> <h2 style='font-size: 1rem; line-height: 1rem; font-weight:bold; margin-block: 56px 20px; color:rgba(0,0,0,0.56);'>Mangler du tilgang til Min Side for Arbeidsgiver hos NAV?</h2> <div style='font-size: 1rem; padding:24px 24px; border-radius: 12px; background-color:#F7F7F7;'> <p style='padding-block: 0px; margin-block-end: 0.5rem; margin-block-start: 0px'>Tilgangen til NAVs rekrutteringstjenester styrer arbeidsgivere selv i <b>Altinn</b>.</p> <p style='margin-block-end: 0.5rem;'>For å få tilgang må du kontakte den som styrer tilgangene til virksomheten din. Det kan være noen i HR, en leder, mellomleder, eller noen på eiersiden i virksomheten.</p> <p style='margin-block-end: 0.5rem;'>Vi har lagd en oppskrift du kan dele med vedkommende for å gjøre det enklere for dem å gi deg tilgang.</p> <h3 style='font-size: 1rem;line-height: 1rem; font-weight:500; margin-block: 40px 32px'>Kopier den gjerne og send den til vedkommende:</h3> <div style='font-size: 1rem; padding:24px 24px; border-radius: 12px; border: 3px dashed rgba(0, 0, 0, 0.1); background-color:#fff;'> <p style='padding-block: 0px; margin-block-end: 0.5rem; margin-block-start: 0px'>Du får denne meldingen fordi avsender ønsker å få tilgang til CV-er fra NAV på vegne av virksomheten din. </p> <b>Gi tilganger til CV-er fra NAV:</b> <ol> <li>Logg inn i Altinn</li> <li>Velg virksomheten din under 'Alle dine aktører'</li> <li>Trykk på knappen 'Profil' øverst i menyen</li> <li>Trykk på 'Andre med rettigheter til virksomheten'</li> <li>Velg 'Legge til ny person eller virksomhet'</li> <li>Legg inn personnummeret og etternavnet til personen som skal ha tilgang</li> <li>Velg 'Gi tilgang til enkelttjenester'</li> <li>Skriv 'Rekruttering', så vil alternativet komme opp som et valg. Velg 'Rekruttering'.</li> <li>Bekreft</li> </ol> <p> Denne enkeltrettigheten gir kun tilgang til å bruke NAV sine rekrutteringstjenester: publisere stillingsannonser og holde videomøter for stillinger på Arbeidsplassen, og motta CV-er fra NAV på <a href='#' style='text-decoration:none; color:#000; cursor: default; font-weight: bold;'>https://arbeidsgiver.nav.no/min-side-arbeidsgiver</a>. </p> <b>Ga ikke Altinn deg muligheten til å gi tilgang?</b> <p>Du kan gi tilgang hvis du har en av disse rollene:</p> <ul> <li>Du er registrert i Enhetsregisteret som daglig leder, styrets leder, bestyrende reder eller innehaver.</li> <li>Du er registert som hovedadministrator i Altinn.</li> <li>Du er 'Tilgangsstyrer' i Altinn og har én, eller flere av rollene: 'Rekruttering', 'Lønn og personalmedarbeider', eller 'Utfyller/innsender'.</li> </ul> </div> </div> </body></html>", "lenke": "https://presenterte-kandidater.intern.dev.nav.no/kandidatliste/666028e2-d031-4d53-8a44-156efc1a3385?virksomhet=123456789", "tidspunkt": "2023-02-09T10:37:45+01:00", "hardDeleteDuration": "P3M", "notifikasjonTekst": "Din virksomhet har mottatt nye kandidater" } }
                    """.trimIndent()
                )
            )
        )
        assertThat(testRapid.inspektør.size).isZero
    }

    @Test
    fun `Skal ikke feile når notifikasjon-api svarer med at notifikasjonen er duplikat `() {
        val melding = """
            {
              "@event_name": "notifikasjon.cv-delt",
              "notifikasjonsId": "enEllerAnnenId",
              "arbeidsgiversEpostadresser": |"test@testepost.no"],
              "stillingsId": "666028e2-d031-4d53-8a44-156efc1a3385",
              "virksomhetsnummer": "123456789",
              "utførtAvVeilederFornavn": "Veileder",
              "utførtAvVeilederEtternavn": "Veiledersen",
              "tidspunktForHendelse": "2023-02-09T10:37:45.108+01:00",
              "meldingTilArbeidsgiver": "Her har du noen fine kandidater!",
              "stillingstittel": "En fantastisk stilling!"
            }
        """.trimIndent()
        stubDuplisertKallTilNotifikasjonssystemet()

        testRapid.sendTestMessage(melding)
    }

    @Test
    fun `Når vi får errors i svaret fra notifikasjonssystemet skal vi throwe error`() {
        val melding = """
            {
              "@event_name": "notifikasjon.cv-delt",
              "arbeidsgiversEpostadresser": ["test@testepost.no"],
              "notifikasjonsId": "enEllerAnnenId",
              "stillingsId": "666028e2-d031-4d53-8a44-156efc1a3385",
              "virksomhetsnummer": "123456789",
              "utførtAvVeilederFornavn": "Veileder",
              "utførtAvVeilederEtternavn": "Veiledersen",
              "tidspunktForHendelse": "2023-02-09T10:37:45.108+01:00",
              "meldingTilArbeidsgiver": "Her har du noen fine kandidater!",
              "stillingstittel": "En fantastisk stilling!"
            }
        """.trimIndent()
        stubErrorsIResponsFraNotifikasjonApi()

        assertThrows<RuntimeException> {
            testRapid.sendTestMessage(melding)
        }
    }

    @Test
    fun `Når vi får ukjent verdi for notifikasjonssvar skal vi throwe error`() {
        val melding = """
            {
              "@event_name": "notifikasjon.cv-delt",
              "arbeidsgiversEpostadresser": ["test@testepost.no"],
              "notifikasjonsId": "enEllerAnnenId",
              "stillingsId": "666028e2-d031-4d53-8a44-156efc1a3385",
              "virksomhetsnummer": "123456789",
              "utførtAvVeilederFornavn": "Veileder",
              "utførtAvVeilederEtternavn": "Veiledersen",
              "tidspunktForHendelse": "2023-02-09T10:37:45.108+01:00",
              "meldingTilArbeidsgiver": "Her har du noen fine kandidater!",
              "stillingstittel": "En fantastisk stilling!"
            }
        """.trimIndent()
        stubUforventetStatusIResponsFraNotifikasjonApi()

        assertThrows<RuntimeException> {
            testRapid.sendTestMessage(melding)
        }
    }

    @Test
    fun `Skal fjerne tabs og spaces fra epostadresser`() {
        val melding = """
            {
              "@event_name": "notifikasjon.cv-delt",
              "notifikasjonsId": "enEllerAnnenId",
              "arbeidsgiversEpostadresser": [" test@testepost.no    "], 
              "stillingsId": "666028e2-d031-4d53-8a44-156efc1a3385",
              "virksomhetsnummer": "123456789",
              "utførtAvVeilederFornavn": "Veileder",
              "utførtAvVeilederEtternavn": "Veiledersen",
              "tidspunktForHendelse": "2023-02-09T10:37:45.108+01:00",
              "meldingTilArbeidsgiver": "Her\nhar du noen \n fine kandidater med \"hermetegn\".",
              "stillingstittel": "En fantastisk stilling!"
            }
        """.trimIndent()
        stubKallTilNotifikasjonssystemet()

        testRapid.sendTestMessage(melding)

        wiremock.verify(
            1, postRequestedFor(
                urlEqualTo("/api/graphql")
            ).withRequestBody(
                equalTo(
                    " " +
                            """
                        { "query": "mutation OpprettNyBeskjed( ${pesostegn}eksternId: String! ${pesostegn}grupperingsId: String! ${pesostegn}merkelapp: String! ${pesostegn}virksomhetsnummer: String! ${pesostegn}epostTittel: String! ${pesostegn}epostBody: String! ${pesostegn}lenke: String! ${pesostegn}tidspunkt: ISO8601DateTime! ${pesostegn}hardDeleteDuration: ISO8601Duration! ${pesostegn}notifikasjonTekst: String! ${pesostegn}epostadresse1: String! ) { nyBeskjed ( nyBeskjed: { metadata: { virksomhetsnummer: ${pesostegn}virksomhetsnummer eksternId: ${pesostegn}eksternId opprettetTidspunkt: ${pesostegn}tidspunkt grupperingsid: ${pesostegn}grupperingsId hardDelete: { om: ${pesostegn}hardDeleteDuration } } mottaker: { altinn: { serviceEdition: \"1\" serviceCode: \"5078\" } } notifikasjon: { merkelapp: ${pesostegn}merkelapp tekst: ${pesostegn}notifikasjonTekst lenke: ${pesostegn}lenke } eksterneVarsler: [ { epost: { epostTittel: ${pesostegn}epostTittel epostHtmlBody: ${pesostegn}epostBody mottaker: { kontaktinfo: { epostadresse: ${pesostegn}epostadresse1 } } sendetidspunkt: { sendevindu: LOEPENDE } } } ] } ) { __typename ... on NyBeskjedVellykket { id } ... on Error { feilmelding } } }", "variables": { "epostadresse1": "test@testepost.no", "eksternId": "enEllerAnnenId", "grupperingsId": "666028e2-d031-4d53-8a44-156efc1a3385", "merkelapp": "Kandidater", "virksomhetsnummer": "123456789", "epostTittel": "Kandidater fra NAV", "epostBody": "<html> <head> <meta http-equiv='Content-Type' content='text/html; charset=UTF-8'/> <title>En fantastisk stilling!</title> </head> <body style='font-family: sans-serif; padding:40px 20px; color: #262626'> <div> <h1 style='font-size: 1.75rem; font-weight:bold;'>Hei.</h1> <h2 style='font-weight: 500; font-size: 1rem;'>Vi har funnet nye kandidater for deg til stillingen: <b>En fantastisk stilling!</b>.</h2> <pre style='font-family: unset;'>Her<br/>har du noen <br/> fine kandidater med \"hermetegn\".</pre> </div> <div style='padding-block-start:32px;'> <h3 style='font-size: 1rem; line-height: 1rem; font-weight:bold;'>Se kandidatene dine</h3> <div style='font-size: 1rem; padding: 12px 16px; border-radius: 12px; background-color:#F7F7F7;'> <p> <ol style='margin-block: 0px; padding-inline-start: 24px;'> <li style='margin-block-end: 0.5rem;'>Åpne <a href='#' style='text-decoration:none; color:#000; cursor: default; font-weight: bold;'>https://arbeidsgiver.nav.no/min-side-arbeidsgiver</a></li> <li style='margin-block-end: 0.5rem;'><b>Logg inn</b></li> <li>Finn <b>varslene dine</b> oppe til høyre på siden, og trykk deg inn på meldingen, eller finn lenken i kortet med teksten <b>Kandidater til mine stillinger</b> lenger ned på siden.</li> </ol> </p> </div> </div> <p style='padding-block:40px 32px'>Vennlig hilsen Veileder Veiledersen</p> <div style='border:1px solid rgba(0, 0, 0, 0.1)'></div> <h2 style='font-size: 1rem; line-height: 1rem; font-weight:bold; margin-block: 56px 20px; color:rgba(0,0,0,0.56);'>Mangler du tilgang til Min Side for Arbeidsgiver hos NAV?</h2> <div style='font-size: 1rem; padding:24px 24px; border-radius: 12px; background-color:#F7F7F7;'> <p style='padding-block: 0px; margin-block-end: 0.5rem; margin-block-start: 0px'>Tilgangen til NAVs rekrutteringstjenester styrer arbeidsgivere selv i <b>Altinn</b>.</p> <p style='margin-block-end: 0.5rem;'>For å få tilgang må du kontakte den som styrer tilgangene til virksomheten din. Det kan være noen i HR, en leder, mellomleder, eller noen på eiersiden i virksomheten.</p> <p style='margin-block-end: 0.5rem;'>Vi har lagd en oppskrift du kan dele med vedkommende for å gjøre det enklere for dem å gi deg tilgang.</p> <h3 style='font-size: 1rem;line-height: 1rem; font-weight:500; margin-block: 40px 32px'>Kopier den gjerne og send den til vedkommende:</h3> <div style='font-size: 1rem; padding:24px 24px; border-radius: 12px; border: 3px dashed rgba(0, 0, 0, 0.1); background-color:#fff;'> <p style='padding-block: 0px; margin-block-end: 0.5rem; margin-block-start: 0px'>Du får denne meldingen fordi avsender ønsker å få tilgang til CV-er fra NAV på vegne av virksomheten din. </p> <b>Gi tilganger til CV-er fra NAV:</b> <ol> <li>Logg inn i Altinn</li> <li>Velg virksomheten din under 'Alle dine aktører'</li> <li>Trykk på knappen 'Profil' øverst i menyen</li> <li>Trykk på 'Andre med rettigheter til virksomheten'</li> <li>Velg 'Legge til ny person eller virksomhet'</li> <li>Legg inn personnummeret og etternavnet til personen som skal ha tilgang</li> <li>Velg 'Gi tilgang til enkelttjenester'</li> <li>Skriv 'Rekruttering', så vil alternativet komme opp som et valg. Velg 'Rekruttering'.</li> <li>Bekreft</li> </ol> <p> Denne enkeltrettigheten gir kun tilgang til å bruke NAV sine rekrutteringstjenester: publisere stillingsannonser og holde videomøter for stillinger på Arbeidsplassen, og motta CV-er fra NAV på <a href='#' style='text-decoration:none; color:#000; cursor: default; font-weight: bold;'>https://arbeidsgiver.nav.no/min-side-arbeidsgiver</a>. </p> <b>Ga ikke Altinn deg muligheten til å gi tilgang?</b> <p>Du kan gi tilgang hvis du har en av disse rollene:</p> <ul> <li>Du er registrert i Enhetsregisteret som daglig leder, styrets leder, bestyrende reder eller innehaver.</li> <li>Du er registert som hovedadministrator i Altinn.</li> <li>Du er 'Tilgangsstyrer' i Altinn og har én, eller flere av rollene: 'Rekruttering', 'Lønn og personalmedarbeider', eller 'Utfyller/innsender'.</li> </ul> </div> </div> </body></html>", "lenke": "https://presenterte-kandidater.intern.dev.nav.no/kandidatliste/666028e2-d031-4d53-8a44-156efc1a3385?virksomhet=123456789", "tidspunkt": "2023-02-09T10:37:45+01:00", "hardDeleteDuration": "P3M", "notifikasjonTekst": "Din virksomhet har mottatt nye kandidater" } }
                    """.trimIndent()
                )
            )
        )
        assertThat(testRapid.inspektør.size).isZero
    }

    fun stubKallTilNotifikasjonssystemet() {
        wiremock.stubFor(
            post("/api/graphql")
                .withHeader("Authorization", containing("Bearer $accessToken"))
                .willReturn(
                    ok(
                        """
                        {
                          "data": {
                            "nyBeskjed": {
                              "__typename": "NyBeskjedVellykket",
                              "id": "79c444d8-c658-43f8-8bfe-fabe668c6dcb"
                            }
                          }
                        }
                    """.trimIndent()
                    ).withHeader("Content-Type", "application/json")
                )
        )
    }

    fun stubDuplisertKallTilNotifikasjonssystemet() {
        wiremock.stubFor(
            post("/api/graphql")
                .withHeader("Authorization", containing("Bearer $accessToken"))
                .willReturn(
                    ok(
                        """
                        {
                          "data": {
                            "nyBeskjed": {
                              "__typename": "DuplikatEksternIdOgMerkelapp",
                              "feilmelding": "notifikasjon med angitt eksternId og merkelapp finnes fra før"
                            }
                          }
                        }
                    """.trimIndent()
                    ).withHeader("Content-Type", "application/json")
                )
        )
    }

    fun stubUforventetStatusIResponsFraNotifikasjonApi() {
        wiremock.stubFor(
            post("/api/graphql")
                .withHeader("Authorization", containing("Bearer $accessToken"))
                .willReturn(
                    ok(
                        """
                        {
                          "data": {
                            "nyBeskjed": {
                              "__typename": "UforventetStatus",
                              "feilmelding": "Dette er en artig feil"
                            }
                          }
                        }
                    """.trimIndent()
                    ).withHeader("Content-Type", "application/json")
                )
        )
    }

    fun stubErrorsIResponsFraNotifikasjonApi() {
        wiremock.stubFor(
            post("/api/graphql")
                .withHeader("Authorization", containing("Bearer $accessToken"))
                .willReturn(
                    ok(
                        """
                        {
                          "error": {
                            "errors": [
                              {
                                "message": "Field \"NyBeskjedInput.metadata\" of required type \"MetadataInput!\" was not provided.",
                                "extensions": {
                                  "code": "GRAPHQL_VALIDATION_FAILED",
                                  "exception": {
                                    "stacktrace": [
                                      "GraphQLError: Field \"NyBeskjedInput.metadata\" of required type \"MetadataInput!\" was not provided.",
                                      "    at ObjectValue (/usr/src/app/server/node_modules/graphql/validation/rules/ValuesOfCorrectTypeRule.js:64:13)",
                                      "    at Object.enter (/usr/src/app/server/node_modules/graphql/language/visitor.js:301:32)",
                                      "    at Object.enter (/usr/src/app/server/node_modules/graphql/utilities/TypeInfo.js:391:27)",
                                      "    at visit (/usr/src/app/server/node_modules/graphql/language/visitor.js:197:21)",
                                      "    at validate (/usr/src/app/server/node_modules/graphql/validation/validate.js:91:24)",
                                      "    at validate (/usr/src/app/server/node_modules/apollo-server-core/dist/requestPipeline.js:186:39)",
                                      "    at processGraphQLRequest (/usr/src/app/server/node_modules/apollo-server-core/dist/requestPipeline.js:98:34)",
                                      "    at processTicksAndRejections (node:internal/process/task_queues:96:5)",
                                      "    at async processHTTPRequest (/usr/src/app/server/node_modules/apollo-server-core/dist/runHttpQuery.js:221:30)"
                                    ]
                                  }
                                }
                              }
                            ]
                          }
                        }
                    """.trimIndent()
                    ).withHeader("Content-Type", "application/json")
                )
        )
    }
}
