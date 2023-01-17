package no.nav.arbeidsgiver.toi.arbeidsgiver.notifikasjon

import no.nav.arbeidsgiver.toi.presentertekandidater.notifikasjoner.graphQlSpørringForCvDeltMedArbeidsgiver
import no.nav.arbeidsgiver.toi.presentertekandidater.notifikasjoner.lagEpostBody
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import java.time.LocalDateTime

class NotifikasjonLytter(rapidsConnection: RapidsConnection) : River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate {
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val stillingId = "";
        val virksomhetsnummer = "";
        val epostBody = lagEpostBody(
            tittel = "",
            tekst = "",
            avsender = ""
        )
        val epostMottaker = "";

        val spørring =
            graphQlSpørringForCvDeltMedArbeidsgiver(
                stillingsId = stillingId,
                virksomhetsnummer = virksomhetsnummer,
                epostBody = epostBody,
                tidspunkt = LocalDateTime.now(),
                epostMottaker = epostMottaker
            )


    }
}
