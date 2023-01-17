package no.nav.arbeidsgiver.toi.arbeidsgiver.notifikasjon

import no.nav.arbeidsgiver.toi.presentertekandidater.notifikasjoner.graphQlSpørringForCvDeltMedArbeidsgiver
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TemplatesTest {

    val pesos = "$"
    @Test
    fun `sjekk at vi har riktig format på template`() {
        val expected = """
            { "query": "mutation OpprettNyBeskjed( ${pesos}eksternId: String! ${pesos}grupperingsId: String! ${pesos}merkelapp: String! ${pesos}virksomhetsnummer: String! ${pesos}epostTittel: String! ${pesos}epostBody: String! ${pesos}epostMottaker: String! ${pesos}lenke: String! ${pesos}tidspunkt: ISO8601DateTime! ${pesos}hardDeleteDuration: ISO8601Duration! ${pesos}notifikasjonTekst: String! ${pesos}epostSendetidspunkt: ISO8601LocalDateTime ) { nyBeskjed ( nyBeskjed: { metadata: { virksomhetsnummer: ${pesos}virksomhetsnummer eksternId: ${pesos}eksternId opprettetTidspunkt: ${pesos}tidspunkt grupperingsid: ${pesos}grupperingsId hardDelete: { om: ${pesos}hardDeleteDuration } } mottaker: { altinn: { serviceEdition: \"1\" serviceCode: \"5078\" } } notifikasjon: { merkelapp: ${pesos}merkelapp tekst: ${pesos}notifikasjonTekst lenke: ${pesos}lenke } eksterneVarsler: { epost: { epostTittel: ${pesos}epostTittel epostHtmlBody: ${pesos}epostBody mottaker: { kontaktinfo: { epostadresse: ${pesos}epostMottaker } } sendetidspunkt: { tidspunkt: ${pesos}epostSendetidspunkt } } } } ) { __typename ... on NyBeskjedVellykket { id } ... on Error { feilmelding } } }", "variables": { "eksternId": "9e4526a2-517f-4fe6-84b7-c8b37b3ad1f2", "grupperingsId": "123", "merkelapp": "Kandidater", "virksomhetsnummer": "888", "epostTittel": "Kandidater fra NAV", "epostBody": "testbody", "epostMottaker": "testMottaker", "lenke": "https://presenterte-kandidater.nav.no/kandidatliste/123?virksomhet=888", "tidspunkt": "2023-01-17T13:22:41.005012", "hardDeleteDuration": "P3M", "notifikasjonTekst": "Din virksomhet har mottatt nye kandidater", "epostSendetidspunkt": "2023-01-17T13:22:41.005012" } }
            """.trimIndent()

        val stillingId = "123"
        val virksomhetsnummer = "888"
        val epostBody = "testbody"
        val epostMottaker = "testMottaker"

        assertThat(
            graphQlSpørringForCvDeltMedArbeidsgiver(
                stillingId, virksomhetsnummer, epostBody, epostMottaker
            )
        ).isEqualTo(expected)
    }
}

