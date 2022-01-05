package no.nav.arbeidsgiver.toi.hjemmel

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

class HjemmelLytter(private val rapidsConnection: RapidsConnection) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandKey("aktoerId")
                it.rejectKey("@event_name")
                it.demandKey("ressurs")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val melding = mapOf(
            "aktørId" to packet["aktoerId"].asText().replace(Regex("\\D+"), ""),
            "hjemmel" to packet.fjernMetadataOgKonverter(),
            "@event_name" to "hjemmel",
        )

        val ressurs = packet["ressurs"].asText()
        if(ressurs == "CV_HJEMMEL") {
            log.info("Skal publisere hjemmelmelding")
            val nyPacket = JsonMessage.newMessage(melding).toJson()
            rapidsConnection.publish(nyPacket)
        }
    }

    private fun JsonMessage.fjernMetadataOgKonverter(): JsonNode {
        val jsonNode = jacksonObjectMapper().readTree(this.toJson()) as ObjectNode
        val metadataFelter = listOf("system_read_count", "system_participating_services", "@event_name")
        jsonNode.remove(metadataFelter)
        return jsonNode
    }
}