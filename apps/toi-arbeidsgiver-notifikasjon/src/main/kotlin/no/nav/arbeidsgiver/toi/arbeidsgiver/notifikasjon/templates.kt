package no.nav.arbeidsgiver.toi.presentertekandidater.notifikasjoner

import java.time.LocalDateTime
import java.time.Period
import java.util.*

private val pesostegn = "$"

fun lagEpostBody(tittel: String, tekst: String, avsender: String) = """
     <html>
     <head>
         <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
         <title>$tittel</title>
     </head>
     <body>
     <p>
         Hei.<br/>
         Din bedrift har mottatt en kandidatliste fra NAV: $tittel.<br/>
         Melding fra markedskontakt i NAV:
     </p>
     <p>
         <pre style="font-family: unset;">$tekst</pre>
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

// TODO: Eget tidspunkt for deling, eget tidspunkt for notifikasjon
fun graphQlSpørringForCvDeltMedArbeidsgiver(
    notifikasjonsId: UUID = UUID.randomUUID(),
    stillingsId: String,
    virksomhetsnummer: String,
    epostBody: String,
    epostMottaker: String,
    tidspunkt: LocalDateTime,
    utløperOm: Period = Period.of(0, 3, 0)
) = spørringForCvDeltMedArbeidsgiver(notifikasjonsId, stillingsId, virksomhetsnummer,epostBody, epostMottaker, tidspunkt, utløperOm)
    .replace("\n", "")

private fun spørringForCvDeltMedArbeidsgiver(
    notifikasjonsId: UUID,
    stillingsId: String,
    virksomhetsnummer: String,
    epostBody: String,
    epostMottaker: String,
    tidspunkt: LocalDateTime,
    utløperOm: Period
): String {
    val merkelapp = "Kandidater";
    val epostTittel = "Kandidater fra NAV";
    val lenke = "https://presenterte-kandidater.nav.no/kandidatliste/$stillingsId?virksomhet=$virksomhetsnummer"
    val notifikasjonTekst = "Din virksomhet har mottatt nye kandidater"

    return """
    {
        "query": "mutation OpprettNyBeskjed(
            ${pesostegn}eksternId: String! 
            ${pesostegn}grupperingsId: String! 
            ${pesostegn}merkelapp: String! 
            ${pesostegn}virksomhetsnummer: String! 
            ${pesostegn}epostTittel: String! 
            ${pesostegn}epostBody: String! 
            ${pesostegn}epostMottaker: String! 
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
                        epost: { 
                            epostTittel: ${pesostegn}epostTittel 
                            epostHtmlBody: ${pesostegn}epostBody 
                            mottaker: { 
                                kontaktinfo: { 
                                    epostadresse: ${pesostegn}epostMottaker 
                                } 
                            } 
                            sendetidspunkt: { 
                                tidspunkt: ${pesostegn}epostSendetidspunkt
                            } 
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
            "epostMottaker": "$epostMottaker",
            "lenke": "$lenke",
            "tidspunkt": "$tidspunkt",
            "hardDeleteDuration": "$utløperOm",
            "notifikasjonTekst": "$notifikasjonTekst",
            "epostSendetidspunkt": "$tidspunkt"
        }
    }
""".trimIndent()
}