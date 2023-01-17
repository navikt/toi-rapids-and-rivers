package no.nav.arbeidsgiver.toi.presentertekandidater.notifikasjoner

import no.nav.arbeidsgiver.toi.arbeidsgiver.notifikasjon.TokendingsKlient
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

class Notifikasjon(private val tokendingsKlient: TokendingsKlient) {
    private val skalSletteNotifikasjonOm = Duration.of(3, ChronoUnit.MONTHS)

    fun sendNotifikasjon(
        epostArbeidsgiver: String,
        stillingsId: UUID,
        virksomhetsnummer: String,
        avsender: String,
    ) {
        val epostBody = lagEpostBody(
            tittel = "Todo tittel",
            tekst = "Todo tekst",
            avsender = avsender
        )

        val spørring =
            graphQlSpørringForCvDeltMedArbeidsgiver(
                stillingsId = stillingsId.toString(),
                virksomhetsnummer = virksomhetsnummer,
                epostBody = epostBody,
                tidspunkt = LocalDateTime.now(),
                epostMottaker = epostArbeidsgiver
            )

    }

    private fun lenkeTilKandidatliste() = ""
}
