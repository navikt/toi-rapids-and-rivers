package no.nav.arbeidsgiver.toi.kandidatfeed

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeType
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.rapids_rivers.*
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord

const val topicName = "toi.kandidat-3"

class KandidatfeedLytter(
    rapidsConnection: RapidsConnection,
    private val producer: Producer<String, String>
) :
    River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandKey("aktørId")
                it.demandKey("synlighet")
                behovsListe.forEach(it::interestedIn)
                it.interestedIn("cv")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val synlighetErFerdigBeregnet = packet["synlighet"]["ferdigBeregnet"].asBoolean()

        if (!synlighetErFerdigBeregnet) {
            log.info("Ignorerer kandidat fordi synlighet ikke er ferdig beregnet" + packet["aktørId"])
            return
        }

        val erSynlig = packet["synlighet"]["erSynlig"].asBoolean()

        if(erSynlig && behovsListe.any { packet[it].isMissingNode }) {
            log.info("Ignorerer kandidat fordi dekte behov er ikke løst" + packet["aktørId"])
            return
        }

        val cvPacket = packet["cv"]["opprettCv"]["cv"] ?: packet["cv"]["endreCv"]["cv"] ?: packet["cv"]["slettCv"]["cv"]
        val synlighetFraArbeidsplassen = cvPacket?.get("synligForVeilederSok")?.asBoolean() ?: false

        if (synlighetFraArbeidsplassen && !erSynlig) {
            log.warn("Synlig i følge Arbeidsplassen, men usynlig i følge oss: ${packet["aktørId"].asText()}")
        }

        val aktørId = packet["aktørId"].asText()
        val packetUtenMetadata = packet.fjernMetadataOgKonverter()
        val melding = ProducerRecord(topicName, aktørId, packetUtenMetadata.toString())

        producer.send(melding) { _, exception ->
            if (exception == null) {
                log.info("Sendte kandidat med aktørId $aktørId, synlighet er $erSynlig")
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

private fun JsonMessage.medLøsninger(løsningsListe: List<String>) {
    demandKey("@løsning")
    get("@løsning").fieldNames().asSequence().toList().containsAll(løsningsListe)
}
