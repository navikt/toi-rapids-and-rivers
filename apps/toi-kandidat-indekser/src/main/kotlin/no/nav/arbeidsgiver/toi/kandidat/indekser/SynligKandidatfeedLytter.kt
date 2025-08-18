package no.nav.arbeidsgiver.toi.kandidat.indekser

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsCv
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

const val topicName = "toi.kandidat-3"

class SynligKandidatfeedLytter(
    rapidsConnection: RapidsConnection,
    private val esClient: ESClient
) :
    River.PacketListener {

    private val secureLog = LoggerFactory.getLogger("secureLog")

    init {
        River(rapidsConnection).apply {
            precondition {
                it.requireKey("aktørId")
                it.requireValue("synlighet.erSynlig", true)
                it.requireValue("synlighet.ferdigBeregnet", true)
                it.forbidValue("@slutt_av_hendelseskjede", true)
                behovsListe.forEach(it::requireKey)
            }
            validate {
                it.requireKey("arbeidsmarkedCv")
                it.requireKey("oppfølgingsinformasjon.oppfolgingsenhet", "veileder.veilederId")
                it.interestedIn("oppfølgingsinformasjon.kvalifiseringsgruppe", "oppfølgingsinformasjon.formidlingsgruppe", "oppfølgingsinformasjon.hovedmaal", "siste14avedtak.hovedmaal", "siste14avedtak.innsatsgruppe", "fritattKandidatsøk.fritattKandidatsok", "veileder.veilederinformasjon.visningsNavn", "veileder.veilederinformasjon.epost")
            }
        }.register(this)
    }


    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry
    ) {
        val aktørId = packet["aktørId"].asText()

        esClient.lagreEsCv(EsCv.fraMelding(packet))
        TODO()
        /*producer.send(melding) { _, exception ->
            if (exception == null) {
                log.info("Sendte kandidat med aktørId (se securelog), synlighet er true")
                secureLog.info("Sendte kandidat med aktørId $aktørId, synlighet er true")
            } else {
                log.error("Klarte ikke å sende kandidat med aktørId (se securelog)", exception)
                secureLog.error("Klarte ikke å sende kandidat med aktørId $aktørId", exception)
            }
            packet["@slutt_av_hendelseskjede"] = true
            context.publish(packet.toJson())
        }*/
    }

    private fun konverterTilObjectNode(packet: JsonMessage) =
        jacksonObjectMapper().readTree(packet.toJson()) as ObjectNode

    override fun onError(problems: MessageProblems, context: MessageContext, metadata: MessageMetadata) {
        log.error(problems.toString())
    }

    private fun ObjectNode.fjernMetadataOgKonverter() {
        val metadataFelter = listOf("system_read_count", "system_participating_services", "@event_name")
        this.remove(metadataFelter)
    }
}