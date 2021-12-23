package no.nav.arbeidsgiver.toi.kandidatfeed

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.rapids_rivers.*
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord

class KandidatfeedLytter(
    rapidsConnection: RapidsConnection,
    private val producer: Producer<String, String>,
    private val erProd: Boolean
) :
    River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandKey("aktørId")
                it.demandKey("cv")
                it.demandKey("synlighet")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        if (erProd) {
            if (packet["cv"].isNull) {
                val feilmelding = "CV kan ikke være null for aktørId ${packet["aktørId"]}"

                log.error(feilmelding)
                throw IllegalArgumentException(feilmelding)
            }
        } else {
            val synlighetErFerdigBeregnet = packet["synlighet"]["ferdigBeregnet"].asBoolean()

            if (!synlighetErFerdigBeregnet) {
                log.info("Ignorerer kandidat fordi synlighet ikke er ferdig beregnet" + packet["aktørId"])
                return
            }
        }

        val aktørId = packet["aktørId"].asText()
        val packetUtenMetadata = packet.fjernMetadataOgKonverter()
        val melding = ProducerRecord("toi.kandidat-2", aktørId, packetUtenMetadata.toString())

        producer.send(melding) { _, exception ->
            if (exception == null) {
                log.info("Sendte kandidat med aktørId $aktørId")
            } else {
                log.error("Klarte ikke å sende kandidat med aktørId $aktørId", exception)
            }
        }
    }

    private fun JsonMessage.fjernMetadataOgKonverter(): JsonNode {
        val jsonNode = jacksonObjectMapper().readTree(this.toJson()) as ObjectNode
        val metadataFelter = listOf("system_read_count", "system_participating_services", "@event_name")
        return jsonNode.remove(metadataFelter)
    }
}
