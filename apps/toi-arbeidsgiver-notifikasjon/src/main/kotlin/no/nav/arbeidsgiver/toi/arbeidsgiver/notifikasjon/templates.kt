package no.nav.arbeidsgiver.toi.arbeidsgiver.notifikasjon

import io.micrometer.core.instrument.util.StringEscapeUtils
import java.time.Period
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

const val TITTEL = "{tittel}"
const val TEKST = "{tekst}"
const val AVSENDER = "{avsender}"

val epostTemplate = """
    <!DOCTYPE html>
    <html lang='no'>
        <head>
            <meta http-equiv='Content-Type' content='text/html; charset=UTF-8' />
            <title id='tittel'>$TITTEL</title>
        </head>
        <body style='font-family: sans-serif; padding: 40px 20px; color: #23262a; mso-line-height-rule: exactly;'>
            <h1 style='font-size: 32px; font-weight: bold'>Hei.</h1>
            <p>Vi har funnet nye kandidater for deg til stillingen: <b id='stillingstittel'>$TITTEL</b>.</p>
            <pre style='font-family: sans-serif; margin-top: 32px; margin-bottom: 32px' id='tekst'>$TEKST</pre>
            <h3 style='font-size: 16px'>For å se kandidatene dine</h3>
            <div style='padding: 16px; margin-bottom: 24px; border-radius: 12px; background-color: #f2f3f5;'>
                <ol style='margin: 0px; padding-left: 24px; line-height: 24px'>
                    <li>Åpne <a href='#' style='text-decoration: none; color: #000000; cursor: default; font-weight: bold;'>https://arbeidsgiver.nav.no/min-side-arbeidsgiver</a></li>
                    <li><b>Logg inn</b></li>
                    <li>Finn <b>varslene dine</b> oppe til høyre på siden, og trykk deg inn på meldingen, eller finn lenken i kortet med teksten <b>Kandidater til mine stillinger</b> lenger ned på siden.</li>
                </ol>
            </div>
            <p>Vennlig hilsen <span id='avsender'>$AVSENDER</span></p>
    
            <div style='border: 1px solid #cbcfd5; margin-top: 32px; margin-bottom: 32px'></div>
    
            <h2 style='font-size: 16px; margin-bottom: 24px'>Mangler du tilgang til Min Side for Arbeidsgiver hos NAV?</h2>
            <div style='padding: 24px; border-radius: 12px; background-color: #f2f3f5'>
                <p style='margin-top: 0'>Tilgangen til NAVs rekrutteringstjenester styrer arbeidsgivere selv i <b>Altinn</b>.</p>
                <p>For å få tilgang må du kontakte den som styrer tilgangene til virksomheten din. Det kan være noen i HR, en leder, mellomleder, eller noen på eiersiden i virksomheten.</p>
                <p>Vi har lagd en oppskrift du kan dele med vedkommende for å gjøre det enklere for dem å gi deg tilgang.</p>
                <p>Kopier den gjerne og send den til vedkommende:</p>
    
                <div style='padding: 24px; border: 3px dashed #cbcfd5; border-radius: 12px; background-color: #ffffff;'>
                    <p style='margin-top: 0'>Du får denne meldingen fordi avsender ønsker å få tilgang til CV-er fra NAV på vegne av virksomheten din.</p>
                    <p><b>Gi tilganger til CV-er fra NAV:</b></p>
                    <ol style='line-height: 24px'>
                        <li>Logg inn i Altinn</li>
                        <li>Velg virksomheten din under «Alle dine aktører»</li>
                        <li>Trykk på knappen «Profil» øverst i menyen</li>
                        <li>Trykk på «Andre med rettigheter til virksomheten»</li>
                        <li>Velg «Legge til ny person eller virksomhet»</li>
                        <li>Legg inn personnummeret og etternavnet til personen som skal ha tilgang</li>
                        <li>Velg «Gi tilgang til enkelttjenester»</li>
                        <li>Skriv «Rekruttering», så vil alternativet komme opp som et valg. Velg «Rekruttering».</li>
                        <li>Bekreft</li>
                    </ol>
                    <p>Denne enkeltrettigheten gir kun tilgang til å bruke NAV sine rekrutteringstjenester: publisere stillingsannonser og holde videomøter for stillinger på Arbeidsplassen, og motta CV-er fra NAV på <a href='#' style='text-decoration: none; color: #000; cursor: default; font-weight: bold;'>https://arbeidsgiver.nav.no/min-side-arbeidsgiver</a>.</p>
                    <p><b>Ga ikke Altinn deg muligheten til å gi tilgang?</b></p>
                    <p>Du kan gi tilgang hvis du har en av disse rollene:</p>
                    <ul style='line-height: 24px'>
                        <li>Du er registrert i Enhetsregisteret som daglig leder, styrets leder, bestyrende reder eller innehaver.</li>
                        <li>Du er registert som hovedadministrator i Altinn.</li>
                        <li>Du er «Tilgangsstyrer» i Altinn og har én, eller flere av rollene: «Rekruttering», «Lønn og personalmedarbeider», eller «Utfyller/innsender».</li>
                    </ul>
                </div>
            </div>
        </body>
    </html>
""".trimIndent()

fun lagEpostBody(tittel: String, tekst: String, avsender: String): String {
    return epostTemplate
        .replace(TITTEL, tittel.htmlEscape())
        .replace(TEKST, tekst.htmlEscape())
        .replace(AVSENDER, avsender)
}

fun graphQlSpørringForCvDeltMedArbeidsgiver(
    notifikasjonsId: String,
    stillingsId: String,
    virksomhetsnummer: String,
    epostBody: String,
    mottakerEpostAdresser: List<String>,
    tidspunktForVarsel: ZonedDateTime,
) =
    spørringForCvDeltMedArbeidsgiver(
        notifikasjonsId,
        stillingsId,
        virksomhetsnummer,
        epostBody,
        mottakerEpostAdresser,
        tidspunktForVarsel
    )
        .replace("\n", "")
        .utenLangeMellomrom()

private fun spørringForCvDeltMedArbeidsgiver(
    notifikasjonsId: String,
    stillingsId: String,
    virksomhetsnummer: String,
    epostBody: String,
    mottakerEpostAdresser: List<String>,
    tidspunktForVarsel: ZonedDateTime,
): String {
    val merkelapp = "Kandidater";
    val epostTittel = "Kandidater fra NAV";

    val erProd = System.getenv()["NAIS_CLUSTER_NAME"] == "prod-gcp"
    val hostprefix = if (erProd) "arbeidsgiver" else "presenterte-kandidater.intern.dev"
    val lenke = "https://$hostprefix.nav.no/kandidatliste/$stillingsId?virksomhet=$virksomhetsnummer"
    val notifikasjonTekst = "Din virksomhet har mottatt nye kandidater"
    val utløperOm = Period.of(0, 3, 0)
    val tidspunktForVarselISO8601DateTime =
        tidspunktForVarsel.truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

    val s = "$"

    return """
    {
        "query": "mutation OpprettNyBeskjed(
            ${s}eksternId: String! 
            ${s}grupperingsId: String! 
            ${s}merkelapp: String! 
            ${s}virksomhetsnummer: String! 
            ${s}epostTittel: String! 
            ${s}epostBody: String! 
            ${s}lenke: String! 
            ${s}tidspunkt: ISO8601DateTime! 
            ${s}hardDeleteDuration: ISO8601Duration!
            ${s}notifikasjonTekst: String!
            ${
        mottakerEpostAdresser.mapIndexed { idx, it ->
            """
                        ${s}epostadresse${idx + 1}: String!
                    """.trimIndent()
        }.joinToString("\n ")
    }
            ) { 
            nyBeskjed (
                nyBeskjed: { 
                    metadata: { 
                        virksomhetsnummer: ${s}virksomhetsnummer
                        eksternId: ${s}eksternId
                        opprettetTidspunkt: ${s}tidspunkt
                        grupperingsid: ${s}grupperingsId
                        hardDelete: { 
                            om: ${s}hardDeleteDuration 
                        } 
                    } 
                    mottaker: { 
                        altinn: { 
                            serviceEdition: \"1\" 
                            serviceCode: \"5078\" 
                        } 
                    } 
                    notifikasjon: { 
                        merkelapp: ${s}merkelapp 
                        tekst:  ${s}notifikasjonTekst 
                        lenke: ${s}lenke 
                    } 
                    eksterneVarsler: [
                        ${
        mottakerEpostAdresser.mapIndexed { idx, it ->
            """
                                {
                                    epost: { 
                                        epostTittel: ${s}epostTittel
                                        epostHtmlBody: ${s}epostBody
                                        mottaker: { 
                                            kontaktinfo: { 
                                                epostadresse: ${s}epostadresse${idx + 1}
                                            } 
                                        } 
                                        sendetidspunkt: { 
                                            sendevindu: LOEPENDE
                                        } 
                                    } 
                                }
                            """.trimIndent()
        }.joinToString(", ")
    }
                    ] 
                } 
            ) { 
            __typename 
            ... on NyBeskjedVellykket { 
                id 
            } 
            ... on Error { 
                feilmelding 
            } 
          }
        }",
        "variables": { 
                    ${
        mottakerEpostAdresser.mapIndexed { idx, verdi ->
            """
                        "epostadresse${idx + 1}": "${verdi.fjernTabsOgSpaces()}",
                    """.trimIndent()
        }.joinToString("\n ")
    }
            "eksternId": "$notifikasjonsId", 
            "grupperingsId": "$stillingsId", 
            "merkelapp": "$merkelapp",
            "virksomhetsnummer": "$virksomhetsnummer",
            "epostTittel": "$epostTittel",
            "epostBody": "$epostBody",
            "lenke": "$lenke",
            "tidspunkt": "$tidspunktForVarselISO8601DateTime",
            "hardDeleteDuration": "$utløperOm",
            "notifikasjonTekst": "$notifikasjonTekst"
        }
    }
""".trimIndent()
}

tailrec fun String.utenLangeMellomrom(): String =
    if (contains("  "))
        replace("  ", " ").utenLangeMellomrom()
    else this

private fun String.htmlEscape(): String =
    replace("\n", "<br/>")
        .let { StringEscapeUtils.escapeJson(it) }

private fun String.fjernTabsOgSpaces(): String =
    replace("\t", "").replace(" ", "")

