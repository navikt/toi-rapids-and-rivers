package no.nav.arbeidsgiver.toi.arenafritattkandidatsok

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
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
        val fnr = fnr(packet) ?: return logManglendeFnr(packet)
        val operasjonstype = operasjonstype(packet) ?: return logManglendeOperasjonstype(packet)

        log.info("Skal publisere arenafritattkandidatsok-melding")
        secureLog.info("Skal publisere arenafritattkandidatsok med fnr $fnr operasjonstype $operasjonstype: ${packet.toJson()}")

        val data = if (operasjonstype == "D") packet["before"] else packet["after"]

        if (data.isNull) {
            logManglendeData(operasjonstype, fnr)
            throw RuntimeException("Mangler data for operasjnstype $operasjonstype, se securelog")
        }

        val fritatt = mapJsonNodeToFritatt(data, packet, operasjonstype == "D")
        fritattRepository.upsertFritatt(fritatt)
        secureLog.info("Oppdaterte $fnr: $fritatt")
    }

    private fun logManglendeFnr(packet: JsonMessage) {
        log.error("Melding fra Arena med FRKAS-kode mangler, se securelog")
        secureLog.error("Melding fra Arena med FRKAS-kode mangler fødselnummer. melding= ${packet.toJson()}")
    }

    private fun logManglendeOperasjonstype(packet: JsonMessage) {
        log.error("Melding fra Arena med operasjonstype mangler, se securelog")
        secureLog.error("Melding fra Arena med operasjonstype mangler operasjonstype. melding= ${packet.toJson()}")
    }

    private fun logManglendeData(operasjonstype: String, fnr: String) {
        secureLog.error("Operasjon $operasjonstype mangler data for fnr $fnr")
        log.error("Operasjon $operasjonstype mangler data")
    }

    private fun mapJsonNodeToFritatt(data: JsonNode, originalmelding: JsonMessage, slettet: Boolean): Fritatt {
        val now = ZonedDateTime.now()
        val id = data["PERSON_ID"].asInt()
        val fnr = data["FODSELSNR"].asText()
        val startDato = localIsoDate(data["START_DATO"].asText().substring(0, 10))
        val sluttDato = data["SLUTT_DATO"].tekstEllerNull()?.let { localIsoDate(it.substring(0, 10)) }
        val endretDatoString = data["ENDRET_DATO"].asText()
        val endretDato = LocalDateTime.parse(endretDatoString, arenaTidsformat).atOsloSameInstant()

        return Fritatt(
            id = id,
            fnr = fnr,
            startdato = startDato,
            sluttdato = sluttDato,
            sendingStatusAktivert = "ikke_sendt",
            forsoktSendtAktivert = null,
            sendingStatusDeaktivert = "ikke_sendt",
            forsoktSendtDeaktivert = null,
            sistEndretIArena = endretDato,
            slettetIArena = slettet,
            opprettetRad = now,
            sistEndretRad = now,
            meldingFraArena = originalmelding.toJson()
        )
    }

    private fun fnr(packet: JsonMessage): String? {
        return packet["after"]["FODSELSNR"]?.asText() ?: packet["before"]["FODSELSNR"]?.asText()
    }

    private fun operasjonstype(packet: JsonMessage): String? {
        return packet["op_type"].asText()
    }

    private fun localIsoDate(input: String) = LocalDate.parse(input, DateTimeFormatter.ISO_LOCAL_DATE)


    private fun JsonNode.tekstEllerNull() = this.takeIf { !it.isNull }?.asText()

}

