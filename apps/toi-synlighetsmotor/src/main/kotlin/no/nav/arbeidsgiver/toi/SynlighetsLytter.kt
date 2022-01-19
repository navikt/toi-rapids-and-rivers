package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.*

class SynlighetsLytter(private val rapidsConnection: RapidsConnection) : River.PacketListener {
    private val interessanteFelt = listOf(
        "cv",
        "oppfølgingsinformasjon",
        "oppfølgingsperiode",
        "fritattKandidatsøk",
        "hjemmel",
        "måBehandleTidligereCv"
    )

    init {
        River(rapidsConnection).apply {
            validate {
                it.interestedIn(*interessanteFelt.toTypedArray())
                it.interestedIn("aktørId")
                it.demandKey("system_participating_services")
                it.rejectKey("synlighet")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val harIngenInteressanteFelter = interessanteFelt.map(packet::get).all(JsonNode::isMissingNode)
        val erSammenstillt = packet["system_participating_services"]
            .map { it.get("service").asText() }
            .contains("toi-sammenstille-kandidat")

        if (harIngenInteressanteFelter || !erSammenstillt) return

        val kandidat = Kandidat.fraJson(packet)
        val synlighet = Synlighet(erSynlig(kandidat), harBeregningsgrunnlag(kandidat))

        packet["synlighet"] = synlighet

        val aktørId = packet["aktørId"].asText()
        log.info("Beregnet synlighet for kandidat $aktørId: $synlighet")

        rapidsConnection.publish(aktørId, packet.toJson())
    }

    private data class Synlighet(val erSynlig: Boolean, val ferdigBeregnet: Boolean)
}
