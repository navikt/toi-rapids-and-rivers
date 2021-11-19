package no.nav.arbeidsgiver.toi.veileder

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

class VeilederLytter(private val rapidsConnection: RapidsConnection): River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandKey("aktorId")
                it.demandKey("veilederId")
                it.rejectKey("@event_name")
            }
        }.register(this)
    }
    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        packet["@event_name"] = "veileder"
        packet["aktørId"] = packet["aktorId"]

        rapidsConnection.publish(packet.toJson())
    }
}