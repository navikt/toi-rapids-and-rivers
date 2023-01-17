package no.nav.arbeidsgiver.toi.arbeidsgiver.notifikasjon

import no.nav.arbeidsgiver.toi.presentertekandidater.notifikasjoner.NotifikasjonKlient
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import java.util.*

class NotifikasjonLytter(rapidsConnection: RapidsConnection) : River.PacketListener {
    private val notifikasjonKlient = NotifikasjonKlient()

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "notifikasjon.cv-delt")
                it.requireKey(
                    "virksomhetsnummer",
                    "stillingsId",
                    "utførendeVeilederFornavn",
                    "utførendeVeilederEtternavn",
                    "mottakerEpost"
                )
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val stillingsId = UUID.fromString(packet["stillingsId"].asText())
        val virksomhetsnummer = packet["virksomhetsnummer"].asText()
        val utførendeVeilederFornavn = packet["utførendeVeilederFornavn"].asText()
        val utførendeVeilederEtternavn = packet["utførendeVeilederEtternavn"].asText()
        val mottakerEpost = packet["mottakerEpost"].asText()

        notifikasjonKlient.sendNotifikasjon(
            mottakerEpost = mottakerEpost,
            stillingsId = stillingsId,
            virksomhetsnummer = virksomhetsnummer,
            avsender = "$utførendeVeilederFornavn $utførendeVeilederEtternavn"
        )
    }
}
