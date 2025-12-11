package no.nav.arbeidsgiver.toi.kandidatfeed

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

const val topicName = "toi.kandidat-3"

class SynligKandidatfeedLytter(
    rapidsConnection: RapidsConnection,
    private val producer: Producer<String, String>
) :
    River.PacketListener {

    private val secureLog = LoggerFactory.getLogger("secureLog")

    init {
        River(rapidsConnection).apply {
            precondition {
                it.requireKey("aktørId")
                it.requireValue("synlighet.erSynlig", true)
                it.requireValue("synlighet.ferdigBeregnet", true)
                it.forbidValue("@slutt_av_hendelseskjede", true)
                behovsListe.forEach(it::requireKey)
            }
            validate {

                it.requireKey("oppfølgingsinformasjon")
            }
        }.register(this)
    }


    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry
    ) {

        val objectNode = konverterTilObjectNode(packet)
            .apply {
                fjernMetadataOgKonverter()
            }

        val aktørId = objectNode["aktørId"].asText()
        val melding = ProducerRecord(topicName, aktørId, objectNode.toString())

        producer.send(melding) { _, exception ->
            if (exception == null) {
                log.info("Sendte kandidat med aktørId (se securelog), synlighet er true")
                secureLog.info("Sendte kandidat med aktørId $aktørId, synlighet er true")
            } else {
                log.error("Klarte ikke å sende kandidat med aktørId (se securelog)", exception)
                secureLog.error("Klarte ikke å sende kandidat med aktørId $aktørId", exception)
            }
            packet["@slutt_av_hendelseskjede"] = true
            //context.publish(packet.toJson())
        }
    }

    private fun konverterTilObjectNode(packet: JsonMessage) =
        jacksonObjectMapper().readTree(packet.toJson()) as ObjectNode

    override fun onError(problems: MessageProblems, context: MessageContext, metadata: MessageMetadata) {
        log.error(problems.toString())
    }

    private fun ObjectNode.fjernMetadataOgKonverter() {
        val metadataFelter = listOf("system_read_count", "system_participating_services", "@event_name")
        this.remove(metadataFelter)
    }
}