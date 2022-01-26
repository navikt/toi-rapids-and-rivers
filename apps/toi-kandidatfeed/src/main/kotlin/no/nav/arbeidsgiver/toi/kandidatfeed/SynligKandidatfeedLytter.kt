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

         val flyttet = flyttOrganisasjonsenhetsnavn(packet)

        val aktørId = flyttet["aktørId"].asText()
        val packetUtenMetadata = flyttet.fjernMetadataOgKonverter()

        val melding = ProducerRecord(topicName, aktørId, packetUtenMetadata.toString())

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

    private fun flyttOrganisasjonsenhetsnavn(packet: JsonMessage): ObjectNode {
        (packet["oppfolgingsinformasjon"] as ObjectNode)
            .set<JsonNode>("organisasjonsenhetsnavn", packet["organisasjonsenhetsnavn"])

        val nynode = jacksonObjectMapper().readTree(packet.toJson()) as ObjectNode
        nynode.remove("organisasjonsenhetsnavn")

        return nynode
    }

    private fun ObjectNode.fjernMetadataOgKonverter(): ObjectNode {
        val metadataFelter = listOf("system_read_count", "system_participating_services", "@event_name")
        return this.remove(metadataFelter)
    }
}