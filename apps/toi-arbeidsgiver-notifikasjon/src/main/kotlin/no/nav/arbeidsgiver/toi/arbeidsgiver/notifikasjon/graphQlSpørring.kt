package no.nav.arbeidsgiver.toi.arbeidsgiver.notifikasjon

import java.time.Period
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

val SAK_MERKELAPP = "Kandidater"
val PESOSTEGN = "$"

fun opprettLenkeTilStilling(stillingsId: String, virksomhetsnummer: String): String {
    val erProd = System.getenv()["NAIS_CLUSTER_NAME"] == "prod-gcp"
    val hostprefix = if (erProd) "arbeidsgiver" else "presenterte-kandidater.intern.dev"

    return "https://$hostprefix.nav.no/kandidatliste/$stillingsId?virksomhet=$virksomhetsnummer"
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
    val epostTittel = "Kandidater fra NAV";
    val lenke = opprettLenkeTilStilling(stillingsId, virksomhetsnummer)
    val notifikasjonTekst = "Din virksomhet har mottatt nye kandidater"

    val utløperOm = Period.of(0, 3, 0)
    val tidspunktForVarselISO8601DateTime =
        tidspunktForVarsel.truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

    return """
    {
        "query": "mutation OpprettNyBeskjed(
            ${PESOSTEGN}eksternId: String! 
            ${PESOSTEGN}grupperingsId: String! 
            ${PESOSTEGN}merkelapp: String! 
            ${PESOSTEGN}virksomhetsnummer: String! 
            ${PESOSTEGN}epostTittel: String! 
            ${PESOSTEGN}epostBody: String! 
            ${PESOSTEGN}lenke: String! 
            ${PESOSTEGN}tidspunkt: ISO8601DateTime! 
            ${PESOSTEGN}hardDeleteDuration: ISO8601Duration!
            ${PESOSTEGN}notifikasjonTekst: String!
            ${
        mottakerEpostAdresser.mapIndexed { idx, it ->
            """
                        ${PESOSTEGN}epostadresse${idx + 1}: String!
                    """.trimIndent()
        }.joinToString("\n ")
    }
            ) { 
            nyBeskjed (
                nyBeskjed: { 
                    metadata: { 
                        virksomhetsnummer: ${PESOSTEGN}virksomhetsnummer
                        eksternId: ${PESOSTEGN}eksternId
                        opprettetTidspunkt: ${PESOSTEGN}tidspunkt
                        grupperingsid: ${PESOSTEGN}grupperingsId
                        hardDelete: { 
                            om: ${PESOSTEGN}hardDeleteDuration 
                        } 
                    } 
                    mottaker: { 
                        altinn: { 
                            serviceEdition: \"1\" 
                            serviceCode: \"5078\" 
                        } 
                    } 
                    notifikasjon: { 
                        merkelapp: ${PESOSTEGN}merkelapp 
                        tekst:  ${PESOSTEGN}notifikasjonTekst 
                        lenke: ${PESOSTEGN}lenke 
                    } 
                    eksterneVarsler: [
                        ${
        mottakerEpostAdresser.mapIndexed { idx, it ->
            """
                                {
                                    epost: { 
                                        epostTittel: ${PESOSTEGN}epostTittel
                                        epostHtmlBody: ${PESOSTEGN}epostBody
                                        mottaker: { 
                                            kontaktinfo: { 
                                                epostadresse: ${PESOSTEGN}epostadresse${idx + 1}
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
            "merkelapp": "$SAK_MERKELAPP",
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

fun graphQlSpørringForSakHosArbeidsgiver(stillingsId: UUID?, stillingstittel: String, organisasjonsnummer: String): String {
    val lenkeTilStilling = opprettLenkeTilStilling(stillingsId.toString(), organisasjonsnummer)

    val query = """
        mutation OpprettNySak(
            ${PESOSTEGN}grupperingsid: String!
            ${PESOSTEGN}virksomhetsnummer: String!
            ${PESOSTEGN}tittel: String!
            ${PESOSTEGN}lenke: String!
        ) {
            nySak(
                grupperingsid: ${PESOSTEGN}grupperingsid
                merkelapp: "Kandidater"
                virksomhetsnummer: ${PESOSTEGN}virksomhetsnummer
                mottakere: [
                    altinn: { 
                        serviceEdition: "1"
                        serviceCode: "5078"
                    } 
                ]
                tittel: ${PESOSTEGN}tittel
                lenke: ${PESOSTEGN}lenke
                initiellStatus: AKTIV_REKRUTTERINGSPROSESS
                overstyrStatustekstMed: "Aktiv rekrutteringsprosess"
            ) {
                __typename
                ... on NySakVellykket {
                    id
                }
                ... on Error {
                    feilmelding
                }
            }
        }
    """

    val variables = """
        {
            "grupperingsid": "$stillingsId"
            "virksomhetsnummer": "$organisasjonsnummer"
            "tittel": "$stillingstittel"
            "lenke": "$lenkeTilStilling"
        }
    """

    return """
        {
            "query": $query,
            "variables": $variables
        }
    """.trimIndent().utenLangeMellomrom()
}

tailrec fun String.utenLangeMellomrom(): String =
    if (contains("  "))
        replace("  ", " ").utenLangeMellomrom()
    else this

private fun String.fjernTabsOgSpaces(): String =
    replace("\t", "").replace(" ", "")

