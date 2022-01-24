package no.nav.arbeidsgiver.toi.kandidatfeed

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord

class UferdigKandidatLytter(
    rapidsConnection: RapidsConnection
) :
    River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandKey("aktørId")
                it.demandValue("synlighet.erSynlig", true)
                it.demandValue("synlighet.ferdigBeregnet", true)
                it.rejectKey("@final")
                it.rejectKey("@behov.kandidatfeed")
                it.interestedIn("cv")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val aktørId = packet["aktørId"].asText()
        packet["@behov.kandidatfeed"] = listOf("organisasjonsenhet")
        context.publish(aktørId, packet.toJson())
    }

    private fun JsonMessage.fjernMetadataOgKonverter(): JsonNode {
        val jsonNode = jacksonObjectMapper().readTree(this.toJson()) as ObjectNode
        val metadataFelter = listOf("system_read_count", "system_participating_services", "@event_name")
        return jsonNode.remove(metadataFelter)
    }
}
