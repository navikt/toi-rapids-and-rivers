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

        val aktørId = packet["aktorId"].asText()
        try {
            val ident = packet["veilederId"].asText()
            val veilederinformasjon = nomKlient.hentVeilederinformasjon(ident)
            packet["veilederinformasjon"] = veilederinformasjon?.toJsonNode() ?: JsonNodeFactory.instance.nullNode()


            val melding = mapOf(
                "aktørId" to aktørId,
                "veileder" to packet.fjernMetadataOgKonverter(),
                "@event_name" to "veileder",
            )

            val nyPacket = JsonMessage.newMessage(melding)


            log.info("Skal publisere veiledermelding for aktørId (se securelog)")
            secureLog.info("Skal publisere veiledermelding for aktørId $aktørId ident $ident")
            rapidsConnection.publish(aktørId, nyPacket.toJson())
        } catch (t:Throwable) {
            log.error("Feil i lesing av hendelse (se securelog)")
            secureLog.error("Feil i lesing av hendelse for aktørId $aktørId", t)
            throw t
        }
    }

    private fun JsonMessage.fjernMetadataOgKonverter(): JsonNode {
        val jsonNode = jacksonObjectMapper().readTree(this.toJson()) as ObjectNode
        val metadataFelter =
            listOf("system_read_count", "system_participating_services", "@event_name", "@id", "@opprettet")
        jsonNode.remove(metadataFelter)
        return jsonNode
    }
}