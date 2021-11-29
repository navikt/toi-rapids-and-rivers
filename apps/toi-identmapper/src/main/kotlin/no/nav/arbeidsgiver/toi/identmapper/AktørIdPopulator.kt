package no.nav.arbeidsgiver.toi.identmapper

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

class AktørIdPopulator(
    private val fnrKey: String,
    private val rapidsConnection: RapidsConnection,
    private val pdlKlient: PdlKlient
) :
    River.PacketListener {
    private val aktørIdKey = "aktørId"

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandKey(fnrKey)
                it.rejectKey(aktørIdKey, "aktorId", "aktoerId")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        packet[aktørIdKey] = pdlKlient.aktørIdFor(packet[fnrKey].asText())
        rapidsConnection.publish(packet.toJson())
    }
}
