package no.nav.arbeidsgiver.toi.hullicv

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.*

class HullICvLytter(rapidsConnection: RapidsConnection) :
    River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandAtFørstkommendeUløsteBehovEr("hullICv")
                it.requireKey("cv")
                it.requireKey("aktørId")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val aktørid: String = packet["aktørId"].asText()
        val cv = packet["cv"]


        //context.publish(aktørid, packet.toJson())
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        log.error(problems.toString())
    }
}

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