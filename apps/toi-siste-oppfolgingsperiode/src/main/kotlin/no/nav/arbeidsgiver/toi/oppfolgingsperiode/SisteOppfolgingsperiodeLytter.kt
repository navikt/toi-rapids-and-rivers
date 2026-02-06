package no.nav.arbeidsgiver.toi.oppfolgingsperiode

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

class SisteOppfolgingsperiodeLytter(private val rapidsConnection: RapidsConnection) : River.PacketListener {

    private val secureLog = LoggerFactory.getLogger("secureLog")

    init {
        River(rapidsConnection).apply {
            precondition{
                it.requireKey("kontor")
                it.requireKey("aktorId")
                it.requireKey("startTidspunkt")
                it.requireKey("ident")
                it.requireKey("sisteEndringsType")
                it.requireKey("oppfolgingsperiodeUuid")
                it.requireKey("producerTimestamp")
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
            "sisteOppfølgingsperiode" to packet.fjernMetadataOgKonverter(),
            "@event_name" to "sisteOppfølgingsperiode",
        )

        val aktørId = packet["aktorId"].asText()
        log.info("Skal publisere siste oppfølgingsperiodemelding for aktørid (se securelog)")
        secureLog.info("Skal publisere siste oppfølgingsperiodemelding for $aktørId")

        val nyPacket = JsonMessage.newMessage(melding)
        rapidsConnection.publish(aktørId, nyPacket.toJson())
    }

    private fun JsonMessage.fjernMetadataOgKonverter(): JsonNode {
        val jsonNode = jacksonObjectMapper().readTree(this.toJson()) as ObjectNode
        val metadataFelter =
            listOf("system_read_count", "system_participating_services", "@event_name", "@id", "@opprettet")
        jsonNode.remove(metadataFelter)
        return jsonNode
    }
}
