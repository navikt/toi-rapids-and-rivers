package no.nav.arbeidsgiver.toi.hjemmel

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

class HjemmelLytter(private val rapidsConnection: RapidsConnection) : River.PacketListener {

    private val secureLog = LoggerFactory.getLogger("secureLog")

    init {
        River(rapidsConnection).apply {
            precondition{
                it.requireKey("aktoerId")
                it.forbid("@event_name")
                it.requireValue("ressurs", "CV_HJEMMEL")
            }
        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry
    ) {
        val aktørId = packet["aktoerId"].asText().extractDigits()

        val melding = mapOf(
            "aktørId" to aktørId,
            "hjemmel" to packet.fjernMetadataOgKonverter(),
            "@event_name" to "hjemmel",
        )

        log.info("Skal publisere hjemmelmelding for aktørId (se securelog)")
        secureLog.info("Skal publisere hjemmelmelding for aktørId $aktørId")
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

fun String.extractDigits() =
    replace(Regex("\\D+"), "")
