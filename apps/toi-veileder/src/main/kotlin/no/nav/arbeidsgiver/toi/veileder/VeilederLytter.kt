package no.nav.arbeidsgiver.toi.veileder

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

class VeilederLytter(private val rapidsConnection: RapidsConnection) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandKey("aktorId")
                it.demandKey("veilederId")
                it.rejectKey("@event_name")
                it.interestedIn("tilordnet")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val melding = mapOf(
            "aktørId" to packet["aktorId"],
            "veileder" to packet.fjernMetadataOgKonverter(),
            "@event_name" to "veileder",
        )
        val nyPacket = JsonMessage.newMessage(melding)
        rapidsConnection.publish(nyPacket.toJson())
    }

    private fun JsonMessage.fjernMetadataOgKonverter(): JsonNode {
        val jsonNode = jacksonObjectMapper().readTree(this.toJson())
        val alleFelter = jsonNode.fieldNames().asSequence().toList()
        val metadataFelter = listOf("system_read_count", "system_participating_services", "@event_name")
        val aktuelleFelter = alleFelter.filter { !metadataFelter.contains(it) }

        val rotNode = jacksonObjectMapper().createObjectNode()

        aktuelleFelter.forEach {
            rotNode.set<JsonNode>(it, jacksonObjectMapper().valueToTree(this[it]))
        }
        return rotNode
    }
}