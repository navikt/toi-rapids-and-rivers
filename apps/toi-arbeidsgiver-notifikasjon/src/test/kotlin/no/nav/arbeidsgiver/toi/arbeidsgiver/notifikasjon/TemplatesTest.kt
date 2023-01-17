package no.nav.arbeidsgiver.toi.arbeidsgiver.notifikasjon

import no.nav.arbeidsgiver.toi.presentertekandidater.notifikasjoner.graphQlSpørringForCvDeltMedArbeidsgiver
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.Period
import java.util.UUID
import kotlin.math.exp

class TemplatesTest {

    private val pesos = "$"

    @Test
    fun `sjekk at vi har riktig format på template`() {
        val tidspunkt = LocalDateTime.now()
        val notifikasjonsId = UUID.randomUUID()
        val stillingId = "123"
        val virksomhetsnummer = "888"
        val epostBody = "testbody"
        val epostMottaker = "testMottaker"
        val utløperOm = Period.of(0, 3, 0)

        val expected = """
            { "query": "mutation OpprettNyBeskjed( ${pesos}eksternId: String! ${pesos}grupperingsId: String! ${pesos}merkelapp: String! ${pesos}virksomhetsnummer: String! ${pesos}epostTittel: String! ${pesos}epostBody: String! ${pesos}epostMottaker: String! ${pesos}lenke: String! ${pesos}tidspunkt: ISO8601DateTime! ${pesos}hardDeleteDuration: ISO8601Duration! ${pesos}notifikasjonTekst: String! ${pesos}epostSendetidspunkt: ISO8601LocalDateTime ) { nyBeskjed ( nyBeskjed: { metadata: { virksomhetsnummer: ${pesos}virksomhetsnummer eksternId: ${pesos}eksternId opprettetTidspunkt: ${pesos}tidspunkt grupperingsid: ${pesos}grupperingsId hardDelete: { om: ${pesos}hardDeleteDuration } } mottaker: { altinn: { serviceEdition: \"1\" serviceCode: \"5078\" } } notifikasjon: { merkelapp: ${pesos}merkelapp tekst: ${pesos}notifikasjonTekst lenke: ${pesos}lenke } eksterneVarsler: { epost: { epostTittel: ${pesos}epostTittel epostHtmlBody: ${pesos}epostBody mottaker: { kontaktinfo: { epostadresse: ${pesos}epostMottaker } } sendetidspunkt: { tidspunkt: ${pesos}epostSendetidspunkt } } } } ) { __typename ... on NyBeskjedVellykket { id } ... on Error { feilmelding } } }", "variables": { "eksternId": "$notifikasjonsId", "grupperingsId": "$stillingId", "merkelapp": "Kandidater", "virksomhetsnummer": "$virksomhetsnummer", "epostTittel": "Kandidater fra NAV", "epostBody": "$epostBody", "epostMottaker": "$epostMottaker", "lenke": "https://presenterte-kandidater.nav.no/kandidatliste/$stillingId?virksomhet=$virksomhetsnummer", "tidspunkt": "$tidspunkt", "hardDeleteDuration": "$utløperOm", "notifikasjonTekst": "Din virksomhet har mottatt nye kandidater", "epostSendetidspunkt": "$tidspunkt" }}
            """.trimIndent()

        assertThat(
            graphQlSpørringForCvDeltMedArbeidsgiver(
                notifikasjonsId, stillingId, virksomhetsnummer, epostBody, epostMottaker, tidspunkt, utløperOm
            )
        ).isEqualTo(expected)
    }
}

