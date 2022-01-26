package no.nav.arbeidsgiver.toi.organisasjonsenhet

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.*

class OrganisasjonsenhetLytter(private val organisasjonsMap: Map<String, String>, rapidsConnection: RapidsConnection) :
    River.PacketListener {
    private val publish: (String) -> Unit = rapidsConnection::publish

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandAtFørstkommendeUløsteBehovEr("organisasjonsenhetsnavn")
                it.requireKey("oppfølgingsinformasjon.oppfolgingsenhet")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val enhetsnummer: String = packet["oppfølgingsinformasjon.oppfolgingsenhet"].asText()

        packet["organisasjonsenhetsnavn"] =
            organisasjonsMap[enhetsnummer] ?: throw Exception("Mangler mapping for enhet $enhetsnummer")

        publish(packet.toJson())
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