package no.nav.arbeidsgiver.toi.siste14avedtak

import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode
import tools.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import no.nav.arbeidsgiver.toi.logging.TeamLogLogger.Companion.teamlog
import no.nav.arbeidsgiver.toi.logging.log

class Siste14aVedtakLytter(private val rapidsConnection: RapidsConnection) : River.PacketListener {
    private val teamlog = teamlog(log)

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

        log.info("Skal publisere siste14aVedtakmelding med aktørid (se teamlog) og fattetDato ${packet["fattetDato"].asString()}")
        teamlog.info("Skal publisere siste14aVedtakmelding med aktørid ${packet["aktorId"].asString()} og fattetDato ${packet["fattetDato"].asString()}")

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
