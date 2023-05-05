package no.nav.arbeidsgiver.toi.arenafritattkandidatsok

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.*
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
        val operasjonstype = operasjonstype(packet) ?: feilMedManglendeOperasjonstype(packet)
        val data = getData(operasjonstype, packet)
        validerData(data, operasjonstype, packet.toJson())
        val fnr = data["FODSELSNR"]?.asTextNullable() ?: feilMedManglendeFnr(packet)

        log.info("Skal publisere arenafritattkandidatsok-melding")
        secureLog.info("Skal publisere arenafritattkandidatsok med fnr $fnr operasjonstype $operasjonstype: ${packet.toJson()}")


        val fritatt = mapJsonNodeToFritatt(data, packet, operasjonstype)
        fritattRepository.upsertFritatt(fritatt)
        secureLog.info("Oppdaterte $fnr: $fritatt")
    }

    private fun validerData(data: JsonNode, operasjonstype: String, melding: String) {
        if (data.isMissingOrNull()) {
            logManglendeData(operasjonstype, melding)
            throw RuntimeException("Mangler data for operasjnstype $operasjonstype, se securelog")
        }
    }

    private fun getData(operasjonstype: String, packet: JsonMessage) =
        if (operasjonstype == "D") packet["before"] else packet["after"]

    private fun feilMedManglendeFnr(packet: JsonMessage): Nothing {
        log.error("Melding fra Arena med FRKAS-kode mangler, se securelog")
        secureLog.error("Melding fra Arena med FRKAS-kode mangler fødselnummer. melding= ${packet.toJson()}")
        throw Exception("Melding fra Arena med FRKAS-kode mangler")
    }

    private fun feilMedManglendeOperasjonstype(packet: JsonMessage): Nothing {
        log.error("Melding fra Arena med operasjonstype mangler, se securelog")
        secureLog.error("Melding fra Arena med operasjonstype mangler operasjonstype. melding= ${packet.toJson()}")
        throw Exception("Melding fra Arena med operasjonstype mangler")
    }

    private fun logManglendeData(operasjonstype: String, melding: String) {
        secureLog.error("Operasjon $operasjonstype mangler, i melding: $melding")
        log.error("Operasjon $operasjonstype mangler data")
    }

    private fun mapJsonNodeToFritatt(data: JsonNode, originalmelding: JsonMessage, operasjonstype: String) =
        Fritatt(
            id = data["PERSON_ID"].asInt(),
            fnr = data["FODSELSNR"].asText(),
            startdato = localIsoDate(data["START_DATO"].asText().substring(0, 10)),
            sluttdato = data["SLUTT_DATO"].asTextNullable()?.let { localIsoDate(it.substring(0, 10)) },
            sendingStatusAktivert = "ikke_sendt",
            forsoktSendtAktivert = null,
            sendingStatusDeaktivert = "ikke_sendt",
            forsoktSendtDeaktivert = null,
            sistEndretIArena = LocalDateTime.parse(data["ENDRET_DATO"].asText(), arenaTidsformat).atOsloSameInstant(),
            slettetIArena = operasjonstype == "D",
            opprettetRad = ZonedDateTime.now(),
            sistEndretRad = ZonedDateTime.now(),
            meldingFraArena = originalmelding.toJson()
        )

    private fun operasjonstype(packet: JsonMessage): String? = packet["op_type"].asTextNullable()

    private fun localIsoDate(input: String) = LocalDate.parse(input, DateTimeFormatter.ISO_LOCAL_DATE)

    private fun JsonNode.asTextNullable(): String? = asText(null)

}
