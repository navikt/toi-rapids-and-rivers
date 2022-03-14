package no.nav.arbeidsgiver.toi.hullicv

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.*

class OrganisasjonsenhetLytter(private val organisasjonsMap: Map<String, String>, rapidsConnection: RapidsConnection) :
    River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandAtFørstkommendeUløsteBehovEr("organisasjonsenhetsnavn")
                it.requireKey("oppfølgingsinformasjon.oppfolgingsenhet")
                it.requireKey("aktørId")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val enhetsnummer: String = packet["oppfølgingsinformasjon.oppfolgingsenhet"].asText()
        val aktørid: String = packet["aktørId"].asText()

        val orgnavn = organisasjonsMap[enhetsnummer]
        if (orgnavn == null) {
            log.error("Mangler mapping for enhet $enhetsnummer på aktørid: $aktørid, setter navn lik tom string")
            packet["organisasjonsenhetsnavn"] = ""
        } else {
            packet["organisasjonsenhetsnavn"] = orgnavn
        }
        log.info("Sender løsning på behov for aktørid: $aktørid enhet: $enhetsnummer ${organisasjonsMap[enhetsnummer]?:""}")

        context.publish(aktørid, packet.toJson())
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