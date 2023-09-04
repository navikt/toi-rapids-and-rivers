package no.nav.arbeidsgiver.toi.veileder

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

class VeilederLytter(private val rapidsConnection: RapidsConnection, private val nomKlient: NomKlient) :
    River.PacketListener {

    private val secureLog = LoggerFactory.getLogger("secureLog")

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandKey("aktorId")
                it.demandKey("veilederId")
                it.rejectKey("@event_name")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val ident = packet["veilederId"].asText()
        val veilederinformasjon = nomKlient.hentVeilederinformasjon(ident)
        packet["veilederinformasjon"] =
            if (veilederinformasjon == null) JsonNodeFactory.instance.nullNode() else veilederinformasjon.toJsonNode()


        val melding = mapOf(
            "aktørId" to packet["aktorId"],
            "veileder" to packet.fjernMetadataOgKonverter(),
            "@event_name" to "veileder",
        )

        val nyPacket = JsonMessage.newMessage(melding)
        val aktørId = packet["aktorId"].asText()


        log.info("Skal publisere veiledermelding for aktørId (se securelog)")
        secureLog.info("Skal publisere veiledermelding for aktørId $aktørId ident $ident")
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