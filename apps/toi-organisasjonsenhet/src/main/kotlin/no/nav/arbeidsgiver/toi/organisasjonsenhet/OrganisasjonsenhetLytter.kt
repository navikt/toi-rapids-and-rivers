package no.nav.arbeidsgiver.toi.organisasjonsenhet

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.rapids_rivers.*

class OrganisasjonsenhetLytter(private val organisasjonsMap: Map<String, String>, rapidsConnection: RapidsConnection) :
    River.PacketListener {
    private val publish: (String) -> Unit = rapidsConnection::publish

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandAtFørstkommendeUløsteBehovEr("organisasjonsenhet")
                it.requireKey("feltmedorganisasjonsnummer")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        packet["@behov"] = packet["@behov"].toList().let { it.subList(1, it.size) }
        packet.medLøsning("organisasjonsenhet",
            organisasjonsMap[packet["feltmedorganisasjonsnummer"].asText()]
                ?: throw Exception("Organisasjonsenhet ikke funnet")
        )

        publish(packet.toJson())
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        log.error(problems.toString())
    }
}

private fun JsonMessage.medLøsning(key: String, value: Any) {
    if (this["@løsning"].isMissingOrNull()) {
        this["@løsning"] = jacksonObjectMapper().readTree("{}")
    }
    (this["@løsning"] as ObjectNode).replace(key, jacksonObjectMapper().valueToTree(value))

}

private fun JsonMessage.demandAtFørstkommendeUløsteBehovEr(informasjonsElement: String) {
    this.interestedIn("@løsning")
    this.demand("@behov") { behovNode ->
        if (behovNode.toList().map(JsonNode::asText).first { this["@løsning"][it] == null } != informasjonsElement)
            throw Exception("Uinteressant hendelse")
    }
}