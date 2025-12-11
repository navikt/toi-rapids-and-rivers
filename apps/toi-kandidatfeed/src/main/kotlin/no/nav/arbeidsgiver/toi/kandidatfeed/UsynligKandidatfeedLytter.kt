package no.nav.arbeidsgiver.toi.kandidatfeed

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

class UsynligKandidatfeedLytter(
    rapidsConnection: RapidsConnection,
    private val producer: Producer<String, String>
) :
    River.PacketListener {

    private val secureLog = LoggerFactory.getLogger("secureLog")

    init {
        River(rapidsConnection).apply {
            precondition{
                it.requireKey("aktørId")
                it.requireValue("synlighet.erSynlig", false)
                it.requireValue("synlighet.ferdigBeregnet", true)
                it.forbidValue("@slutt_av_hendelseskjede", true)
            }
        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry
    ) {
        val aktørId = packet["aktørId"].asText()
        val packetUtenMetadata = packet.fjernMetadataOgKonverter()
        val melding = ProducerRecord(topicName, aktørId, packetUtenMetadata.toString())

        producer.send(melding) { _, exception ->
            if (exception == null) {
                log.info("Sendte kandidat med aktørId (se securelog), synlighet er false")
                secureLog.info("Sendte kandidat med aktørId $aktørId, synlighet er false")
            } else {
                log.error("Klarte ikke å sende kandidat med aktørId (se securelog)", exception)
                secureLog.error("Klarte ikke å sende kandidat med aktørId $aktørId", exception)
            }
            packet["@slutt_av_hendelseskjede"] = true
            //context.publish(packet.toJson())
        }
    }

    private fun JsonMessage.fjernMetadataOgKonverter(): JsonNode {
        val jsonNode = jacksonObjectMapper().readTree(this.toJson()) as ObjectNode
        val metadataFelter = listOf("system_read_count", "system_participating_services", "@event_name")
        return jsonNode.remove(metadataFelter)
    }
}
