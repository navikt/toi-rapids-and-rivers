package no.nav.arbeidsgiver.toi.kandidat.indekser

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsCv
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsCv.Companion.indekseringsnøkkel
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

class UsynligKandidatfeedLytter(
    rapidsConnection: RapidsConnection,
    private val esClient: ESClient
) :
    River.PacketListener {

    private val secureLog = LoggerFactory.getLogger("secureLog")

    init {
        River(rapidsConnection).apply {
            precondition{
                it.requireKey("aktørId")
                it.requireValue("synlighet.erSynlig", false)
                it.requireValue("synlighet.ferdigBeregnet", true)
                it.forbidValue("@slutt_av_hendelseskjede", true)
                it.interestedIn("arbeidsmarkedCv")
            }
        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry
    ) {
        val indekseringsnøkkel = indekseringsnøkkel(packet)
        packet["kandidatFunnetOgSlettet"] = if(indekseringsnøkkel==null) {
            false
        } else {
            esClient.slettCv(indekseringsnøkkel)
            true
        }
        packet["@slutt_av_hendelseskjede"] = true
        context.publish(packet.toJson())
    }
}
