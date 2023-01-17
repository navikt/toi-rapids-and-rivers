package no.nav.arbeidsgiver.toi.presentertekandidater.notifikasjoner

import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

class NotifikasjonKlient {

    fun sendNotifikasjon(
        mottakerEpost: String,
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
                mottakerEpost = mottakerEpost
            )

        // TODO: Send notifikasjon

    }
}
