package no.nav.arbeidsgiver.toi.presentertekandidater.notifikasjoner

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


// TODO: Må fjerne newlines
fun lagGraphQlSpørring() = """
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
                            epostSendetidspunkt: { 
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
            "eksternId": "ID", 
            "grupperingsId": "id", 
            "merkelapp": "merkelapp",
            "virksomhetsnummer": "virksomhetsnummer",
            "epostTittel": "tittel",
            "epostBody": "body",
            "epostMottaker": "hei@hei.no",
            "lenke": "lenke",
            "tidspunkt": "2001-12-24T10:44:01",
            "hardDeleteDuration": "P2DT3H4M",
            "notifikasjonTekst": "tekst",
            "epostSendetidspunkt": "2001-12-24T10:44:01"
        }
    }
""".trimIndent()