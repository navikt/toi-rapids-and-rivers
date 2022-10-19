package no.nav.arbeidsgiver.toi.kandidatfeed

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

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
