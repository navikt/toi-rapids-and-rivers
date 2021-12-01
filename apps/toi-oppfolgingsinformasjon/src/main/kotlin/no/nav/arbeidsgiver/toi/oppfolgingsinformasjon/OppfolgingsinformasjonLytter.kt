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
                it.demandKey("fodselsnummer")
                it.demandKey("oppfolgingsenhet")
                it.demandKey("harOppfolgingssak")
                it.rejectKey("@event_name")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val melding = mapOf(
            "fodselsnummer" to packet["fodselsnummer"],
            "oppfølgingsinformasjon" to packet.fjernMetadataOgKonverter(),
            "@event_name" to "oppfølgingsinformasjon",
        )

        log.info("Skal publisere oppfølgingsinformasjonmelding uten aktørId")

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
