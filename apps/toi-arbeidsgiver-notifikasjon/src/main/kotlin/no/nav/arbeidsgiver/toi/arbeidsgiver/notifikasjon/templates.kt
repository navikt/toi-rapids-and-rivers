package no.nav.arbeidsgiver.toi.presentertekandidater.notifikasjoner

private val pesostegn = "$"

fun lagEpostBody() = """ epost """

fun lagGraphQlMutation(
    epostArbeidsgiver: String,
    stillingsId: String,
    stillingstittel: String,
    virksomhetsnummer: String,
) = """
    mutation OpprettNyBeskjed(
      ${pesostegn}grupperingsid: String!
      ${pesostegn}merkelapp: String!
      ${pesostegn}virksomhetsnummer: String!
      ${pesostegn}tittel: String!
      ${pesostegn}lenke: String!
      ${pesostegn}tidspunkt: ISO8601DateTime
      ${pesostegn}hardDeleteDuration: ISO8601Duration
    ) {
      nyBeskjed(
        nyBeskjed: {
          metadata: {
            virksomhetsnummer: ${pesostegn}virksomhetsnummer,
            eksternId: "sdsds"
            opprettetTidspunkt: ${pesostegn}tidspunkt
            grupperingsid: ${pesostegn}grupperingsid
             hardDelete: {
                om: ${pesostegn}hardDeleteDuration
            }
          }
          mottaker: {
            altinn: {
              serviceEdition: "1" 
              serviceCode: "5078"
            } 
          }
          notifikasjon: {
            merkelapp: ${pesostegn}merkelapp
            tekst: "dette er tekst"
            lenke: ${pesostegn}lenke
          }
          eksterneVarsler: {
            epost: {
              epostTittel: ${pesostegn}tittel
              epostHtmlBody: "htmlbody"
              mottaker: { kontaktinfo: { epostadresse: "hei@hei.no" } }
              sendetidspunkt: { tidspunkt: "2001-12-24T10:44:01"}
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
    }
    
""".trimIndent()