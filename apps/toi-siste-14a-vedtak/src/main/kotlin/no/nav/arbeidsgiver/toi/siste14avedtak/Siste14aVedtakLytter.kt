package no.nav.arbeidsgiver.toi.siste14avedtak

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.rapids_rivers.*

class Siste14aVedtakLytter(private val rapidsConnection: RapidsConnection) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandKey("aktorId")
                it.demandKey("innsatsgruppe")
                it.demandKey("hovedmal")
                it.demandKey("fattetDato")
                it.interestedIn("fraArena")
                it.rejectKey("@event_name")
            }
        }.register(this)
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        super.onError(problems, context)
    }

    override fun onSevere(error: MessageProblems.MessageException, context: MessageContext) {
        super.onSevere(error, context)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val melding = mapOf(
            "aktørId" to packet["aktorId"],
            "siste14avedtak" to packet.fjernMetadataOgKonverter(),
            "@event_name" to "siste14avedtak",
        )

        log.info("Skal publisere siste14aVedtakmelding med fattetDato ${packet["fattetDato"]}")

        val nyPacket = JsonMessage.newMessage(melding)
        rapidsConnection.publish(nyPacket.toJson())
    }

    private fun JsonMessage.fjernMetadataOgKonverter(): JsonNode {
        val jsonNode = jacksonObjectMapper().readTree(this.toJson()) as ObjectNode
        val metadataFelter = listOf("system_read_count", "system_participating_services", "@event_name")
        jsonNode.remove(metadataFelter)
        return jsonNode
    }
}
