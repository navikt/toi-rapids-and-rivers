package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.rapids_rivers.*

class KandidatEndretLytter(rapidsConnection: RapidsConnection) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandKey("aktoerId")
                it.rejectKey("@event_name")
                it.requireKey("harTilretteleggingsbehov")
                it.requireKey("behov")
            }
        }.register(this)
    }

    fun utgåendeMelding(aktørId: String, inkommendeMelding: JsonNode): JsonMessage = JsonMessage.newMessage(
        mapOf(
            "@event_name" to "tilretteleggingsbehov",
            "aktørId" to aktørId,
            "tilretteleggingsbehov" to inkommendeMelding
        )
    )

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val aktoerId = packet["aktoerId"].asText()
        val utgåendeMelding = utgåendeMelding(aktoerId, packet.toJsonNode())
        context.publish(aktoerId, utgåendeMelding.toJson())
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        log.error("require har feilet på melding: $problems")
    }

    private fun JsonMessage.toJsonNode() = jacksonObjectMapper().readTree(toJson()) as ObjectNode
}