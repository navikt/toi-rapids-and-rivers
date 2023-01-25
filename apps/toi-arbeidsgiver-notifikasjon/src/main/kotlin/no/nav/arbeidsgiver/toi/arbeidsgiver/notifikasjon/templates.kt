package no.nav.arbeidsgiver.toi.presentertekandidater.notifikasjoner

import java.time.LocalDateTime
import java.time.Period
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val pesostegn = "$"

fun lagEpostBody(tittel: String, tekst: String, avsender: String) = """
     <html>
     <head>
         <meta http-equiv='Content-Type' content='text/html; charset=UTF-8'/>
         <title>$tittel</title>
     </head>
     <body>
     <p>
         Hei.<br/>
         Din bedrift har mottatt en kandidatliste fra NAV: $tittel.<br/>
         Melding fra markedskontakt i NAV:
     </p>
     <p>
         <pre style='font-family: unset;'>$tekst</pre>
     </p>
     <p>
         Logg deg inn på Min side - Arbeidsgiver for å se lista.
     </p>
     <p>
         Mvh, $avsender
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


tailrec fun String.utenLangeMellomrom(): String =
    if (contains("  "))
        replace("  ", " ").utenLangeMellomrom()
    else this

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

    return """
    {
        "query": "mutation OpprettNyBeskjed(
            ${pesostegn}eksternId: String! 
            ${pesostegn}grupperingsId: String! 
            ${pesostegn}merkelapp: String! 
            ${pesostegn}virksomhetsnummer: String! 
            ${pesostegn}epostTittel: String! 
            ${pesostegn}epostBody: String! 
            ${pesostegn}lenke: String! 
            ${pesostegn}tidspunkt: ISO8601DateTime! 
            ${pesostegn}hardDeleteDuration: ISO8601Duration!
            ${pesostegn}notifikasjonTekst: String!
            ${pesostegn}epostSendetidspunkt: ISO8601LocalDateTime
            ) { 
            nyBeskjed (
                nyBeskjed: { 
                    metadata: { 
                        virksomhetsnummer: ${pesostegn}virksomhetsnummer
                        eksternId: ${pesostegn}eksternId
                        opprettetTidspunkt: ${pesostegn}tidspunkt
                        grupperingsid: ${pesostegn}grupperingsId
                        hardDelete: { 
                            om: ${pesostegn}hardDeleteDuration 
                        } 
                    } 
                    mottaker: { 
                        altinn: { 
                            serviceEdition: \"1\" 
                            serviceCode: \"5078\" 
                        } 
                    } 
                    notifikasjon: { 
                        merkelapp: ${pesostegn}merkelapp 
                        tekst:  ${pesostegn}notifikasjonTekst 
                        lenke: ${pesostegn}lenke 
                    } 
                    eksterneVarsler: { 
                        ${
                            mottakerEpostAdresser.map {
                                """
                                {
                                    epost: { 
                                        epostTittel: ${pesostegn}epostTittel
                                        epostHtmlBody: ${pesostegn}epostBody
                                        mottaker: { 
                                            kontaktinfo: { 
                                                epostadresse: $it
                                            } 
                                        } 
                                        sendetidspunkt: { 
                                            tidspunkt: ${pesostegn}epostSendetidspunkt
                                        } 
                                    } 
                                }
                            """.trimIndent()
                            }.joinToString(", ")
                        }
                    } 
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
            "eksternId": "$notifikasjonsId", 
            "grupperingsId": "$stillingsId", 
            "merkelapp": "$merkelapp",
            "virksomhetsnummer": "$virksomhetsnummer",
            "epostTittel": "$epostTittel",
            "epostBody": "$epostBody",
            "lenke": "$lenke",
            "tidspunkt": "$tidspunktForVarselISO8601DateTime",
            "hardDeleteDuration": "$utløperOm",
            "notifikasjonTekst": "$notifikasjonTekst",
            "epostSendetidspunkt": "${LocalDateTime.MIN}"
        }
    }
""".trimIndent()
}
