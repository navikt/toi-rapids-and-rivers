package no.nav.arbeidsgiver.toi.kandidatfeed

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.rapids_rivers.*
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
                it.requireKey("oppfølgingsinformasjon.oppfolgingsenhet")
                it.interestedIn("@behov")
                it.interestedIn("cv")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        if(packet.rejectOnAll("@behov", behovsListe)) return

        val aktørId = packet["aktørId"].asText()
        packet["@behov"] = packet["@behov"].toSet() + behovsListe
        log.info("Sender behov for $aktørId")
        context.publish(aktørId, packet.toJson())
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        log.error(problems.toString())
    }

}

private fun JsonMessage.rejectOnAll(key: String, values: List<String>) = get(key).let { node ->
    !node.isMissingNode && node.isArray && node.map(JsonNode::asText).containsAll(values)
}
