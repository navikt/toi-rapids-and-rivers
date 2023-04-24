package no.nav.arbeidsgiver.toi.arenafritattkandidatsok

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

class ArenaFritattKandidatsokLytter(private val rapidsConnection: RapidsConnection) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("table", "ARENA_GOLDENGATE.ARBEIDSMARKEDBRUKER_FRITAK")
                it.interestedIn("before", "after")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val fnr = fnr(packet)
        if (fnr == null) return

        val melding = mapOf(
            "fodselsnummer" to fnr,
            "arenafritattkandidatsok" to packet.fjernMetadataOgKonverter(),
            "@event_name" to "arenafritattkandidatsok",
        )

        log.info("Skal publisere arenafritattkandidatsok-melding: " + packet.toJson()) // TODO: Ikke i prod, secure-log?

        val nyPacket = JsonMessage.newMessage(melding)
        rapidsConnection.publish(fnr, nyPacket.toJson())
    }

    private fun fnr(packet: JsonMessage): String? {
        val fnr: String? = packet["after"]["FODSELSNR"]?.asText() ?: packet["before"]["FODSELSNR"]?.asText()
        if (fnr == null) {
            log.error("Melding fra Arena med FRKAS-kode mangler fødselnummer. melding=" + packet.toJson())
        }
        return fnr
    }

    private fun JsonMessage.fjernMetadataOgKonverter(): JsonNode {
        val jsonNode = jacksonObjectMapper().readTree(this.toJson()) as ObjectNode
        val metadataFelter =
            listOf("system_read_count", "system_participating_services", "@event_name", "@id", "@opprettet")
        jsonNode.remove(metadataFelter)
        return jsonNode
    }


}
