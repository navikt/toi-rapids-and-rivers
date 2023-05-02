package no.nav.arbeidsgiver.toi.arenafritattkandidatsok

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ArenaFritattKandidatsokLytter(
    rapidsConnection: RapidsConnection,
    private val fritattRepository: FritattRepository,
) : River.PacketListener {

    private val secureLog = LoggerFactory.getLogger("secureLog")

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("table", "ARENA_GOLDENGATE.ARBEIDSMARKEDBRUKER_FRITAK")
                it.interestedIn("before", "after")
                it.interestedIn("op_type")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {

        val fnr = fnr(packet)
        val operasjonstype = operasjonstype(packet)
        if (fnr == null || operasjonstype == null) return
        log.info("Skal publisere arenafritattkandidatsok-melding")

        /*val melding = mapOf(
            "fodselsnummer" to fnr,
            "arenafritattkandidatsok" to packet.fjernMetadataOgKonverter(),
            "@event_name" to "arenafritattkandidatsok",
        )*/

        secureLog.info("Skal publisere arenafritattkandidatsok med fnr ${fnr} operasjonstype ${operasjonstype}: ${packet.toJson()}")

        if (operasjonstype == "D") {
            fritattRepository.slettFritatt(fnr)
            secureLog.info("Slettet $fnr")
            return
        }

        val data = packet["after"];
        if (data == null) {
            secureLog.error("Operasjon $operasjonstype mangler data for fnr $fnr")
            log.error("Operasjon $operasjonstype mangler data")
            return
        }
        val fritatt = mapJsonNodeToFritatt(data, packet)
        fritattRepository.upsertFritatt(fritatt)
        secureLog.info("Oppdaterte $fnr: $fritatt")
    }

    fun mapJsonNodeToFritatt(data: JsonNode, originalmelding: JsonMessage): Fritatt {
        val id = data["PERSON_ID"].asInt()
        val fnr = data["FODSELSNR"].asText()
        val melding = originalmelding.toJson()

        val startDatoString = data["START_DATO"].asText()
        val startDato = LocalDate.parse(startDatoString.substring(0, 10), DateTimeFormatter.ISO_LOCAL_DATE)

        val sluttDatoString = data["SLUTT_DATO"]?.asText()
        val sluttDato = sluttDatoString?.substring(0, 10)?.let { LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE) }

        val endretDatoString = data["ENDRET_DATO"].asText()
        val endretDato = LocalDateTime.parse(endretDatoString, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

        return Fritatt(
            id = id,
            fnr = fnr,
            melding = melding,
            startdato = startDato,
            sluttdato = sluttDato ?: startDato,
            sendingStatusAktivertFritatt = "ikke_sendt",
            forsoktSendtAktivertFritatt = null,
            sendingStatusDektivertFritatt = "ikke_sendt",
            forsoktSendtDektivertFritatt = null,
            sistEndret = endretDato
        )
    }

    private fun fnr(packet: JsonMessage): String? {
        val fnr: String? = packet["after"]["FODSELSNR"]?.asText() ?: packet["before"]["FODSELSNR"]?.asText()
        if (fnr == null) {
            log.error("Melding fra Arena med FRKAS-kode mangler, se securelog")
            secureLog.error("Melding fra Arena med FRKAS-kode mangler fødselnummer. melding= ${packet.toJson()}")
        }
        return fnr
    }

    private fun operasjonstype(packet: JsonMessage): String? {
        val operasjonstype: String? = packet["op_type"].asText()
        if (operasjonstype == null) {
            log.error("Melding fra Arena med operasjonstype mangler, se securelog")
            secureLog.error("Melding fra Arena med operasjonstype mangler operasjonstype. melding= ${packet.toJson()}")
        }
        return operasjonstype
    }

    private fun JsonMessage.fjernMetadataOgKonverter(): JsonNode {
        val jsonNode = jacksonObjectMapper().readTree(this.toJson()) as ObjectNode
        val metadataFelter =
            listOf("system_read_count", "system_participating_services", "@event_name", "@id", "@opprettet")
        jsonNode.remove(metadataFelter)
        return jsonNode
    }


}
