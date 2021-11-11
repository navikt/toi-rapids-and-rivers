package no.nav.arbeidsgiver.toi

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

class CvLytter(rapidsConnection: RapidsConnection, private val behandler: Behandler) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "cv")
                it.demandKey("aktørId")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        behandler.behandleHendelse(Hendelse(CvHendelse, packet["aktørId"].asText() , packet))
    }
}

class VeilederLytter(
    rapidsConnection: RapidsConnection, private val behandler: Behandler
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "veileder")
                it.demandKey("aktørId")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        behandler.behandleHendelse(Hendelse(VeilederHendelse, packet["aktørId"].asText(), packet ))
    }
}