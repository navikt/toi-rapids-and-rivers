package no.nav.arbeidsgiver.toi.oppfolgingsinformasjon

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry

class OppfolgingsinformasjonLytter(private val rapidsConnection: RapidsConnection) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            precondition{
                it.requireKey("fodselsnummer")
                it.requireKey("oppfolgingsenhet")
                it.requireKey("harOppfolgingssak")
                it.forbid("@event_name")
                it.interestedIn("sistEndretDato")
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
            "fodselsnummer" to packet["fodselsnummer"],
            "oppfølgingsinformasjon" to packet.fjernMetadataOgKonverter(),
            "@event_name" to "oppfølgingsinformasjon",
        )

        log.info("Skal publisere oppfølgingsinformasjonmelding med sistEndretDato ${packet["sistEndretDato"]}")

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
