package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)

class SynlighetsgrunnlagLytter(
    private val rapidsConnection: RapidsConnection,
    private val repository: Repository,
    private val objectMapper: ObjectMapper = jacksonObjectMapper()
) : River.PacketListener {

    private val requiredFields = requiredFieldsSynlilghetsbehov()

    init {
        River(rapidsConnection).apply {
            precondition {
                it.forbid("synlighet")
                it.requireAny(requiredFields)
                it.interestedIn("aktørId")
            }
        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry
    ) {
        log.info("Mottatt melding: ${packet.toJson()}")
        val kandidat = Kandidat.fraJson(packet)
        val evaluering = kandidat.toEvaluering()
        log.info("Evaluering: $evaluering")

        // Beregn hvilke felter som finnes og hvilke som mangler
        val presentFields = requiredFields.filter { field ->
            !objectMapper.fromSynlighetsnode<Any>(packet[field]).isMissing
        }
        log.info("Følgende requiredFields finnes: $presentFields")

        val missingFields = requiredFields.filter { field ->
            objectMapper.fromSynlighetsnode<Any>(packet[field]).isMissing
        }
        log.info("Følgende requiredFields mangler: $missingFields")

        if (evaluering.harAktivCv && missingFields.isNotEmpty()) {
            packet["@behov"] = missingFields + presentFields
        }

        // Sett alltid synlighet i meldingen
        packet["synlighet"] = evaluering
        if (!evaluering.erSynlig()) {
            repository.lagre(evaluering, kandidat.aktørId, kandidat.fødselsNummer())
        }
        val output = packet.toJson()
        log.info("Publiserer melding: $output")
        rapidsConnection.publish(kandidat.aktørId, output)
    }

}

private fun JsonMessage.requireAny(keys: List<String>) {
    if (keys.onEach { interestedIn(it) }
            .map(this::get)
            .all { it.isMissingNode }
    )
        throw MessageProblems.MessageException(MessageProblems(toJson()).apply { error("Ingen av feltene fantes i meldingen") })
}
