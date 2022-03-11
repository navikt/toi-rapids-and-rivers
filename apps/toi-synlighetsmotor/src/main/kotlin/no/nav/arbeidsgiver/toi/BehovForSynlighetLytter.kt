package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.databind.JsonNode
import no.nav.arbeidsgiver.toi.Evaluering.Companion.invoke
import no.nav.arbeidsgiver.toi.Evaluering.Companion.lagEvalueringSomObfuskererKandidaterMedDiskresjonskode
import no.nav.helse.rapids_rivers.*

class BehovForSynlighetLytter(
    rapidsConnection: RapidsConnection,
    private val hentEvalueringsGrunnlag: (String) -> Evaluering?
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate{
                it.demandAtFørstkommendeUløsteBehovEr("synlighet")
                it.demandKey("fodselsnummer")
                it.rejectKey("synlighet")
            }
        }
    }
    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val fnr = packet["fodselsnummer"].asText()
        val evalueringsGrunnlag = hentEvalueringsGrunnlag(fnr)
        packet["synlighet"] = evalueringsGrunnlag()
        context.publish(fnr, packet.toJson())
    }
}

private fun JsonMessage.demandAtFørstkommendeUløsteBehovEr(informasjonsElement: String) {
    demand("@behov") { behovNode ->
        if (behovNode
                .toList()
                .map(JsonNode::asText)
                .onEach { interestedIn(it) }
                .first { this[it].isMissingOrNull() } != informasjonsElement
        )
            throw Exception("Uinteressant hendelse")
    }
}