package no.nav.arbeidsgiver.toi.siste14avedtak

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry

class Siste14aVedtakLytter(private val rapidsConnection: RapidsConnection) : River.PacketListener {
    private val secureLog = SecureLog(log)

    init {
        River(rapidsConnection).apply {
            precondition {
                it.requireKey("aktorId")
                it.requireKey("innsatsgruppe")
                it.requireKey("hovedmal")
                it.requireKey("fattetDato")
                it.interestedIn("fraArena")
                it.forbid("@event_name")
            }
        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry
    ) {
        val melding = mapOf(
            "aktørId" to packet["aktorId"],
            "siste14avedtak" to packet.fjernMetadataOgKonverter(),
            "@event_name" to "siste14avedtak",
        )

        log.info("Skal publisere siste14aVedtakmelding med aktørid (se securelog) og fattetDato ${packet["fattetDato"].asText()}")
        secureLog.info("Skal publisere siste14aVedtakmelding med aktørid ${packet["aktorId"].asText()} og fattetDato ${packet["fattetDato"].asText()}")

        val nyPacket = JsonMessage.newMessage(melding)
        rapidsConnection.publish(nyPacket.toJson())
    }

    private fun JsonMessage.fjernMetadataOgKonverter(): JsonNode {
        val jsonNode = jacksonObjectMapper().readTree(this.toJson()) as ObjectNode
        val metadataFelter =
            listOf("system_read_count", "system_participating_services", "@event_name", "@id", "@opprettet")
        jsonNode.remove(metadataFelter)
        return jsonNode
    }
}
