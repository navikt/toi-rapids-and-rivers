package no.nav.arbeidsgiver.toi.arbeidsgiver.notifikasjon

import io.micrometer.core.instrument.util.StringEscapeUtils
import java.time.Period
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

fun lagEpostBody(tittel: String, tekst: String, avsender: String) = """
     <html>
     <head>
         <meta http-equiv='Content-Type' content='text/html; charset=UTF-8'/>
         <title>$tittel</title>
     </head>
     <body>
     <p>
         Hei.<br/>
         Din bedrift har mottatt en kandidatliste fra NAV: ${tittel.htmlEscape()}.<br/>
         Melding fra markedskontakt i NAV:
     </p>
     <p>
         <pre style='font-family: unset;'>${tekst.htmlEscape()}</pre>
     </p>
     <p>
         Logg deg inn på Min side - Arbeidsgiver for å se lista.
     </p>
     <p>
         Mvh. $avsender
     </p>
     </body>
     </html>
""".trimIndent()

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
    val hostprefix = if (erProd) "arbeidsgiver" else "presenterte-kandidater.dev"
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
                        "epostadresse${idx + 1}": "$verdi",
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

