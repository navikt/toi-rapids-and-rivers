package no.nav.arbeidsgiver.toi

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

class SynlighetsLytter(rapidsConnection: RapidsConnection): River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandKey("oppfølgingsinformasjon")
                it.rejectKey("synlighet")
            }
        }.register(this)
    }
    private val publish: (String) -> Unit = rapidsConnection::publish

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        packet["synlighet"] = Synlighet(erSynlig(packet),false)
        publish(packet.toJson())
    }
    private class Synlighet(val erSynlig: Boolean, val ferdigBeregnet: Boolean)
}