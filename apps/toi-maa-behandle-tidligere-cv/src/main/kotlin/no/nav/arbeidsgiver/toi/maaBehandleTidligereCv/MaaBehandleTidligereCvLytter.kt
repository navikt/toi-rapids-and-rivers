package no.nav.arbeidsgiver.toi.maaBehandleTidligereCv

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory

class MaaBehandleTidligereCvLytter(private val rapidsConnection: RapidsConnection) : River.PacketListener {

    private val secureLog = LoggerFactory.getLogger("secureLog")

    init {
        River(rapidsConnection).apply {
            precondition {
                it.requireKey("aktorId")
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

        val aktørId = packet["aktorId"].asText()

        val melding = mapOf(
            "aktørId" to aktørId,
            "måBehandleTidligereCv" to packet.fjernMetadataOgKonverter(),
            "@event_name" to "må-behandle-tidligere-cv",
        )

        log.info("Skal publisere måBehandleTidligereCv-melding for aktørId (se securelog)")
        secureLog.info("Skal publisere måBehandleTidligereCv-melding for aktørId $aktørId")
        val nyPacket = JsonMessage.newMessage(melding).toJson()
        rapidsConnection.publish(aktørId, nyPacket)
    }

    private fun JsonMessage.fjernMetadataOgKonverter(): JsonNode {
        val jsonNode = jacksonObjectMapper().readTree(this.toJson()) as ObjectNode
        val metadataFelter =
            listOf("system_read_count", "system_participating_services", "@event_name", "@id", "@opprettet")
        jsonNode.remove(metadataFelter)
        return jsonNode
    }
}
