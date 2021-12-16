package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.*

class SynlighetsLytter(rapidsConnection: RapidsConnection) : River.PacketListener {
    private val interessanteFelt = listOf("oppfølgingsinformasjon", "cv", "oppfølgingsperiode", "fritattKandidatsøk")

    init {
        River(rapidsConnection).apply {
            validate {
                it.interestedIn(*interessanteFelt.toTypedArray())
                it.demandKey("system_participating_services")
                it.rejectKey("synlighet")
            }
        }.register(this)
    }

    private val publish: (String) -> Unit = rapidsConnection::publish

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val harIngenInteressanteFelter = interessanteFelt.map(packet::get).all(JsonNode::isMissingNode)
        val erSammenstillt =  packet["system_participating_services"]
            .map{it.get("service").asText()}
            .contains("toi-sammenstille-kandidat")

        if (harIngenInteressanteFelter || !erSammenstillt) return

        val kandidat = Kandidat.fraJson(packet)
        packet["synlighet"] = Synlighet(erSynlig(kandidat), harBeregningsgrunnlag(kandidat))

        publish(packet.toJson())
    }

    private class Synlighet(val erSynlig: Boolean, val ferdigBeregnet: Boolean)
}
