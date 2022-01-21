package no.nav.arbeidsgiver.toi.organisasjonsenhet

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

class OrganisasjonsenhetLytter(private val organisasjonsMap: Map<String, String>, rapidsConnection: RapidsConnection) :
    River.PacketListener {
    private val publish: (String) -> Unit = rapidsConnection::publish
    init {
        River(rapidsConnection).apply {
            validate {
                it.demand("@behov", medFørstkommendeBehov("organisasjonsenhet"))
                it.rejectKey("organisasjonsenhet")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        packet["@behov"] = packet["@behov"].toList().let { it.subList(1, it.size) }
        publish(packet.toJson())
    }
}


private fun medFørstkommendeBehov(informasjonsElement: String): (JsonNode) -> Unit = {
    if (it.first().asText() != informasjonsElement)
        throw Exception("Uinteressant hendelse")
}