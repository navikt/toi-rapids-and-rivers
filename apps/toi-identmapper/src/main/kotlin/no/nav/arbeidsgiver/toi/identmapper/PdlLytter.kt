package no.nav.arbeidsgiver.toi.identmapper

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

class PdlLytter(rapidsConnection: RapidsConnection, private val lagre: (String?, String) -> Unit) :
    River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandKey("aktoerId")
                it.demandKey("fnr")
                it.rejectKey("@event_name")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val aktørId = packet["aktoerId"].asText()
        val fnr = packet["fnr"].asText()
        lagre(aktørId, fnr)
    }
}
