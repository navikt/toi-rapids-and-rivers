package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

class SystemCountLytter(rapidsConnection: RapidsConnection): River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandKey("system_read_count")
                it.interestedIn("@event_name")
                it.interestedIn("system_participating_services")
            }
        }.register(this)
    }
    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val systemReadCount = packet["system_read_count"].asInt()
        if(systemReadCount>=10) {
            val eventName = packet["@event_name"].asTextNullable()
            val systemParticipatingServices = packet["system_participating_services"].toPrettyString()
            log.error("System-readcount er stor!! Kan være at hendelser har havnet i loop!!\n" +
                    "system_read_count: $systemReadCount.\n" +
                    "Har eventNavn: $eventName\n" +
                    "system_participating_services: $systemParticipatingServices")
        }
    }

}

fun JsonNode.asTextNullable() = asText(null)