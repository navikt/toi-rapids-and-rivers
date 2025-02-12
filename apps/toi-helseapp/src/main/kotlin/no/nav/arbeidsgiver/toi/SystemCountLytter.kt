package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry

class SystemCountLytter(rapidsConnection: RapidsConnection): River.PacketListener {
    init {
        River(rapidsConnection).apply {
            precondition{
                it.requireKey("system_read_count")
                it.interestedIn("@event_name")
                it.interestedIn("system_participating_services")
            }
        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry
    ) {
        val systemReadCount = packet["system_read_count"].asInt()
        if(systemReadCount>11) {
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