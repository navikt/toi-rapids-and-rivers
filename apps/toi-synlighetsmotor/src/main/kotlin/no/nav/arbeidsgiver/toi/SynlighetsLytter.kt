package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

class SynlighetsLytter(private val rapidsConnection: RapidsConnection, private val repository: Repository) :
    River.PacketListener {
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

        val synlighetsevaluering = lagEvalueringsGrunnlag(kandidat)
        val synlighet = Synlighet(synlighetsevaluering.erSynlig(), synlighetsevaluering.erFerdigBeregnet)

        packet["synlighet"] = synlighet
        val aktørId = packet["aktørId"].asText()
        val fødselsnummer = packet["oppfølgingsinformasjon"]["fodselsnummer"].asText() //TODO: sjekke flere steder for fødselsnummer

        log.info("Beregnet synlighet for kandidat $aktørId: $synlighet")

        repository.lagre(evaluering = synlighetsevaluering, aktørId = aktørId, fødselsnummer = fødselsnummer)

        rapidsConnection.publish(aktørId, packet.toJson())
    }

    private data class Synlighet(val erSynlig: Boolean, val ferdigBeregnet: Boolean)
}
