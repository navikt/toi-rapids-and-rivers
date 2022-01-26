package no.nav.arbeidsgiver.toi.kandidatfeed

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.rapids_rivers.*
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord

const val topicName = "toi.kandidat-3"

class SynligKandidatfeedLytter(
    rapidsConnection: RapidsConnection,
    private val producer: Producer<String, String>
) :
    River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandKey("aktørId")
                it.demandValue("synlighet.erSynlig", true)
                it.demandValue("synlighet.ferdigBeregnet", true)
                it.requireKey("oppfolgingsinformasjon")
                behovsListe.forEach(it::demandKey)
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {

        val objectNode = jacksonObjectMapper().readTree(packet.toJson()) as ObjectNode

        flyttOrganisasjonsenhetsnavn(objectNode)

        val aktørId = objectNode["aktørId"].asText()
       objectNode.fjernMetadataOgKonverter()

        val melding = ProducerRecord(topicName, aktørId, objectNode.toString())

        producer.send(melding) { _, exception ->
            if (exception == null) {
                log.info("Sendte kandidat med aktørId $aktørId, synlighet er true")
            } else {
                log.error("Klarte ikke å sende kandidat med aktørId $aktørId", exception)
            }
        }
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        log.error(problems.toString())
    }

    private fun flyttOrganisasjonsenhetsnavn(packet: ObjectNode) {
        (packet["oppfolgingsinformasjon"] as ObjectNode)
            .set<JsonNode>("organisasjonsenhetsnavn", packet["organisasjonsenhetsnavn"])

        packet.remove("organisasjonsenhetsnavn")
    }

    private fun ObjectNode.fjernMetadataOgKonverter() {
        val metadataFelter = listOf("system_read_count", "system_participating_services", "@event_name")
        this.remove(metadataFelter)
    }
}