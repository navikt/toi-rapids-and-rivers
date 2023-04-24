package no.nav.arbeidsgiver.toi.arenafritattkandidatsok

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

class ArenaFritattKandidatsokLytter(private val rapidsConnection: RapidsConnection) : River.PacketListener {

    private val secureLog = LoggerFactory.getLogger("secureLog")

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
        log.info("Skal publisere arenafritattkandidatsok-melding")

        val melding = mapOf(
            "fodselsnummer" to fnr,
            "arenafritattkandidatsok" to packet.fjernMetadataOgKonverter(),
            "@event_name" to "arenafritattkandidatsok",
        )

        secureLog.info("Skal publisere arenafritattkandidatsok-melding: " + packet.toJson())

        val nyPacket = JsonMessage.newMessage(melding)
        rapidsConnection.publish(fnr, nyPacket.toJson())
    }

    private fun fnr(packet: JsonMessage): String? {
        val fnr: String? = packet["after"]["FODSELSNR"]?.asText() ?: packet["before"]["FODSELSNR"]?.asText()
        if (fnr == null) {
            log.error("Melding fra Arena med FRKAS-kode mangler, se securelog")
            secureLog.error("Melding fra Arena med FRKAS-kode mangler fødselnummer. melding=" + packet.toJson())
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
