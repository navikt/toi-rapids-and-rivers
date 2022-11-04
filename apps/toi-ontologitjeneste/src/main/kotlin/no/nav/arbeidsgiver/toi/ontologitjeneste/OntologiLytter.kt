package no.nav.arbeidsgiver.toi.ontologitjeneste

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.*

class OntologiLytter(ontologiUrl: String, rapidsConnection: RapidsConnection) :
    River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandAtFørstkommendeUløsteBehovEr("ontologi")
                it.requireKey("kompetanse", "stillingstittel")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        packet["ontologi"] = Ontologi(
            packet["kompetanse"].map(JsonNode::asText).associateWith(this::synonymerTilKompetanse),
            packet["stillingstittel"].map(JsonNode::asText).associateWith(this::synonymerTilStillingstittel)
        )
    }

    private fun synonymerTilKompetanse(kompetanse: String): List<String> = TODO()
    private fun synonymerTilStillingstittel(stillingstittel: String): List<String> = TODO()
}

data class Ontologi(val kompetanse: Map<String, List<String>>, val stilling: Map<String, List<String>>)

private fun JsonMessage.demandAtFørstkommendeUløsteBehovEr(informasjonsElement: String) {
    demand("@behov") { behovNode ->
        if (behovNode
                .toList()
                .map(JsonNode::asText)
                .onEach { interestedIn(it) }
                .first { this[it].isMissingOrNull() } != informasjonsElement
        )
            throw Exception("Uinteressant hendelse")
    }
}
