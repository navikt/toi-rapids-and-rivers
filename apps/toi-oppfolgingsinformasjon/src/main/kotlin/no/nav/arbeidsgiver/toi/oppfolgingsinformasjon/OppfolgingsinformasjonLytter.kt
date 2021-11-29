package no.nav.arbeidsgiver.toi.oppfolgingsinformasjon

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

class OppfolgingsinformasjonLytter(private val rapidsConnection: RapidsConnection) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandKey("aktoerid")
                it.demandKey("har_oppfolgingssak")
                it.rejectKey("@event_name")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val aktoerid = packet["aktoerid"]
        val melding = mapOf(
            "aktørId" to aktoerid,
            "oppfølgingsinformasjon" to packet.fjernMetadataOgKonverter(),
            "@event_name" to "oppfølgingsinformasjon",
        )
        val nyPacket = JsonMessage.newMessage(melding)
        log.info("Skal publisere oppfølgingsinformasjonmelding for aktørid til rapid: ${aktoerid.asText()}")
        rapidsConnection.publish(nyPacket.toJson())
    }

    private fun JsonMessage.fjernMetadataOgKonverter(): JsonNode {
        val jsonNode = jacksonObjectMapper().readTree(this.toJson()) as ObjectNode
        val metadataFelter = listOf("system_read_count", "system_participating_services", "@event_name")
        jsonNode.remove(metadataFelter)
        return jsonNode
    }
}