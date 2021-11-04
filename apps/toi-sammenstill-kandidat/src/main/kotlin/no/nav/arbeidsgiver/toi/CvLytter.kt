package no.nav.arbeidsgiver.toi

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

class CvLytter(rapidsConnection: RapidsConnection, private val behandleHendelse: (Hendelse) -> Unit): River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "Kandidat.NyFraArbeidsplassen")
                it.demandKey("aktørid")
            }
        }.register(this)
    }
    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        behandleHendelse(Hendelse(HendelseType.CV, packet["aktørid"].asText() , packet))
    }
}