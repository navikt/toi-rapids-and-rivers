package no.nav.arbeidsgiver.toi.oppfolgingsinformasjon

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.arbeidsgiver.toi.oppfølgingsinforamsjon.log
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

class OppfølgingsinformasjonLytter(private val rapidsConnection: RapidsConnection) : River.PacketListener {
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
        val melding = mapOf(
            "aktørId" to packet["aktorId"],
            "oppfølgingsinformasjon" to packet.fjernMetadataOgKonverter(),
                "@event_name" to "oppfølgingsinformasjon",
        )
        val nyPacket = JsonMessage.newMessage(melding)
        log.info("Skal publisere veiledermelding")
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