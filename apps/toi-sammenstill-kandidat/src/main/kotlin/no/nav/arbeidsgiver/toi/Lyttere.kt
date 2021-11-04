package no.nav.arbeidsgiver.toi

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

class CvLytter(rapidsConnection: RapidsConnection, private val behandler: Behandler) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "Kandidat.NyFraArbeidsplassen")
                it.demandKey("aktørId")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        behandler.behandleHendelse(Hendelse(HendelseType.CV, packet["aktørId"].asText() , packet))
    }
}

class VeilederLytter(
    rapidsConnection: RapidsConnection, private val behandler: Behandler
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "Kandidat.ny_veileder")
                it.demandKey("aktørId")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        behandler.behandleHendelse(Hendelse(HendelseType.VEILEDER, packet["aktørId"].asText(), packet ))
    }
}