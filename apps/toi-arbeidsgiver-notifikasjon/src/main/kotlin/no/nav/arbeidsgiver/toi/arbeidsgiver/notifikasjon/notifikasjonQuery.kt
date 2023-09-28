package no.nav.arbeidsgiver.toi.arbeidsgiver.notifikasjon

import java.time.Period
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

const val SAK_MERKELAPP = "Kandidater"
const val PESOSTEGN = "$"

fun queryOpprettNyBeskjed(
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
    val tidspunktForVarselISO8601DateTime =
        tidspunktForVarsel.truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

    val eposttyper = List(mottakerEpostAdresser.size) { index ->
        "${PESOSTEGN}epostadresse${index + 1}: String!"
    }.joinToString(" ")

    val eposterJson = List(mottakerEpostAdresser.size) { index ->
        """
            {
                epost: {
                    epostTittel: ${PESOSTEGN}epostTittel
                    epostHtmlBody: ${PESOSTEGN}epostBody
                    mottaker: {
                        kontaktinfo: {
                            epostadresse: ${PESOSTEGN}epostadresse${index + 1}
                        }
                    }
                    sendetidspunkt: {
                        sendevindu: LOEPENDE
                    }
                }
            }
        """.trimIndent()
    }.joinToString(", ")

    val query = """
        mutation OpprettNyBeskjed(
            ${PESOSTEGN}eksternId: String!
            ${PESOSTEGN}grupperingsId: String!
            ${PESOSTEGN}merkelapp: String!
            ${PESOSTEGN}virksomhetsnummer: String!
            ${PESOSTEGN}epostTittel: String!
            ${PESOSTEGN}epostBody: String!
            ${PESOSTEGN}lenke: String!
            ${PESOSTEGN}tidspunkt: ISO8601DateTime!
            ${PESOSTEGN}notifikasjonTekst: String!
            $eposttyper
        ) {
            nyBeskjed (
                nyBeskjed: {
                    metadata: {
                        virksomhetsnummer: ${PESOSTEGN}virksomhetsnummer
                        eksternId: ${PESOSTEGN}eksternId
                        opprettetTidspunkt: ${PESOSTEGN}tidspunkt
                        grupperingsid: ${PESOSTEGN}grupperingsId
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
                    eksterneVarsler: [ $eposterJson ]
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
    """

    val epostvariabler = mottakerEpostAdresser.mapIndexed { index, verdi ->
        Pair("epostadresse${index + 1}", verdi.fjernTabsOgSpaces())
    }.toMap()

    return opprettQuery(query, epostvariabler + mapOf(
        "eksternId" to notifikasjonsId,
        "grupperingsId" to stillingsId,
        "merkelapp" to SAK_MERKELAPP,
        "virksomhetsnummer" to virksomhetsnummer,
        "epostTittel" to epostTittel,
        "epostBody" to epostBody,
        "lenke" to lenke,
        "tidspunkt" to tidspunktForVarselISO8601DateTime,
        "notifikasjonTekst" to notifikasjonTekst,
    ))
}

fun queryOpprettNySak(
    stillingsId: UUID?,
    stillingstittel: String,
    organisasjonsnummer: String
): String {
    val lenkeTilStilling = opprettLenkeTilStilling(stillingsId.toString(), organisasjonsnummer)

    val query = """
        mutation OpprettNySak(
            ${PESOSTEGN}grupperingsid: String!,
            ${PESOSTEGN}virksomhetsnummer: String!,
            ${PESOSTEGN}tittel: String!,
            ${PESOSTEGN}lenke: String!,
            ${PESOSTEGN}merkelapp: String!,
            ${PESOSTEGN}initiellStatus: SaksStatus!,
            ${PESOSTEGN}overstyrStatustekstMed: String
        ) {
            nySak(
                grupperingsid: ${PESOSTEGN}grupperingsid,
                merkelapp: ${PESOSTEGN}merkelapp,
                virksomhetsnummer: ${PESOSTEGN}virksomhetsnummer,
                mottakere: [
                    {
                        altinn: {
                            serviceEdition: \"1\",
                            serviceCode: \"5078\"
                        }
                    }
                ],
                tittel: ${PESOSTEGN}tittel,
                lenke: ${PESOSTEGN}lenke,
                initiellStatus: ${PESOSTEGN}initiellStatus,
                overstyrStatustekstMed: ${PESOSTEGN}overstyrStatustekstMed
            ) {
                __typename
                ... on NySakVellykket {
                    id
                }
                ... on DuplikatGrupperingsid {
                    feilmelding
                }
                ... on Error {
                    feilmelding
                }
            }
        }
    """

    return opprettQuery(query, mapOf(
        "grupperingsid" to stillingsId.toString(),
        "virksomhetsnummer" to organisasjonsnummer,
        "tittel" to stillingstittel,
        "lenke" to lenkeTilStilling,
        "merkelapp" to SAK_MERKELAPP,
        "initiellStatus" to "MOTTATT",
        "overstyrStatustekstMed" to "Aktiv rekrutteringsprosess"
    ))
}

fun queryFerdigstillSak(stillingsId: UUID): String {
    val query = """
        mutation FerdigstillSak(
            ${PESOSTEGN}grupperingsid: String!,
            ${PESOSTEGN}merkelapp: String!,
            ${PESOSTEGN}nyStatus: SaksStatus!,
            ${PESOSTEGN}overstyrStatustekstMed: String
        ) {
            nyStatusSakByGrupperingsid(
                grupperingsid: ${PESOSTEGN}grupperingsid,
                merkelapp: ${PESOSTEGN}merkelapp,
                nyStatus: ${PESOSTEGN}nyStatus,
                overstyrStatustekstMed: ${PESOSTEGN}overstyrStatustekstMed
            ) {
                __typename
                ... on NyStatusSakVellykket {
                    id
                }
                ... on Error {
                    feilmelding
                }
            }
        }
    """

    return opprettQuery(query, mapOf(
        "grupperingsid" to stillingsId.toString(),
        "merkelapp" to SAK_MERKELAPP,
        "nyStatus" to "FERDIG",
        "overstyrStatustekstMed" to "Avsluttet rekrutteringsprosess"
    ))
}

fun querySlettSak(stillingsId: UUID): String {
    val query = """
        mutation SlettSak(
            ${PESOSTEGN}grupperingsid: String!,
            ${PESOSTEGN}merkelapp: String!
        ) {
            hardDeleteSakByGrupperingsid(
                grupperingsid: ${PESOSTEGN}grupperingsid,
                merkelapp: ${PESOSTEGN}merkelapp
            ) {
                __typename
                ... on HardDeleteSakVellykket {
                    id
                }
                ... on Error {
                    feilmelding
                }
            }
        }
    """

    return opprettQuery(query, mapOf(
        "grupperingsid" to stillingsId.toString(),
        "merkelapp" to SAK_MERKELAPP,
    ))
}

fun opprettQuery(query: String, variables: Map<String, String>): String {
    val queryJson = """
        {
            "query": "${query.trim()}",
            "variables": {
                ${variables.entries.joinToString(", ") {
                    (key, value) -> "\"$key\": \"$value\""
                }}
            }
        }
    """

    return queryJson.oneLiner()
}

fun opprettLenkeTilStilling(stillingsId: String, virksomhetsnummer: String): String {
    val erProd = System.getenv()["NAIS_CLUSTER_NAME"] == "prod-gcp"
    val hostprefix = if (erProd) "arbeidsgiver" else "presenterte-kandidater.intern.dev"

    return "https://$hostprefix.nav.no/kandidatliste/$stillingsId?virksomhet=$virksomhetsnummer"
}

fun String.oneLiner(): String {
    return this.trim().replace(Regex("\\s+"), " ")
}

private fun String.fjernTabsOgSpaces(): String =
    replace("\t", "").replace(" ", "")

