package no.nav.arbeidsgiver.toi.identmapper

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

class AktorIdPopulator(
    private val fnrKey: String,
    private val rapidsConnection: RapidsConnection,
    private val hentAktørId: (fødselsnummer: String) -> String
) :
    River.PacketListener {
    private val aktørIdKey = "aktørId"

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandKey(fnrKey)
                it.rejectKey(aktørIdKey, "aktorId", "aktoerId")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val fødselsnummer = packet[fnrKey].asText()

        log.info("Mottok melding med fødselsnummer")

        packet[aktørIdKey] = hentAktørId(fødselsnummer)
        rapidsConnection.publish(packet.fjernMetadataOgKonverter().toString())
    }

    private fun JsonMessage.fjernMetadataOgKonverter(): JsonNode {
        val jsonNode = jacksonObjectMapper().readTree(this.toJson()) as ObjectNode
        val metadataFelter = listOf("system_read_count", "system_participating_services", "@event_name")
        jsonNode.remove(metadataFelter)

        return jsonNode
    }
}
