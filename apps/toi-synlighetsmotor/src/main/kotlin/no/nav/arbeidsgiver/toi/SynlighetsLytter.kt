package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.*

class SynlighetsLytter(rapidsConnection: RapidsConnection): River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.interestedIn("oppfølgingsinformasjon", "cv")
                it.rejectKey("synlighet")
            }
        }.register(this)
    }
    private val publish: (String) -> Unit = rapidsConnection::publish

    private val interessanteFelt = listOf("oppfølgingsinformasjon", "cv")

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        if(interessanteFelt.map(packet::get).all(JsonNode::isMissingNode)) return
        packet["synlighet"] = Synlighet(erSynlig(packet),harBeregningsgrunnlag(packet))
        publish(packet.toJson())
    }
    private class Synlighet(val erSynlig: Boolean, val ferdigBeregnet: Boolean)
}