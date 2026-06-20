package no.nav.arbeidsgiver.toi.oppfolgingsperiode

import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode
import tools.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import no.nav.arbeidsgiver.toi.logging.TeamLogLogger.Companion.teamlog
import no.nav.arbeidsgiver.toi.logging.log

class SisteOppfolgingsperiodeLytter(private val rapidsConnection: RapidsConnection) : River.PacketListener {
    private val teamlog = teamlog(log)

    init {
        River(rapidsConnection).apply {
            precondition{
                it.requireKey("oppfolgingsperiodeUuid")
                it.forbid("@event_name")
            }
            validate {
                it.requireKey("aktorId")
                it.requireKey("startTidspunkt")
                it.requireKey("ident")
                it.requireKey("sisteEndringsType")
                it.requireKey("producerTimestamp")
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
            "sisteOppfølgingsperiode" to packet.fjernMetadataOgKonverter(),
            "@event_name" to "sisteOppfølgingsperiode",
        )

        val aktørId = packet["aktorId"].asString()
        log.info("Skal publisere siste oppfølgingsperiodemelding for aktørid (se teamlog)")
        teamlog.info("Skal publisere siste oppfølgingsperiodemelding for $aktørId")

        val nyPacket = JsonMessage.newMessage(melding)
        rapidsConnection.publish(aktørId, nyPacket.toJson())
    }

    override fun onError(problems: MessageProblems, context: MessageContext, metadata: MessageMetadata) {
        log.error(problems.toString())
        teamlog.error(problems.toExtendedReport())
    }

    private fun JsonMessage.fjernMetadataOgKonverter(): JsonNode {
        val jsonNode = jacksonObjectMapper().readTree(this.toJson()) as ObjectNode
        val metadataFelter =
            listOf("system_read_count", "system_participating_services", "@event_name", "@id", "@opprettet")
        jsonNode.remove(metadataFelter)
        return jsonNode
    }
}
