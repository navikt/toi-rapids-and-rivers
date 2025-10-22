package no.nav.arbeidsgiver.toi.kandidat.indekser

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory

class UsynligKandidatfeedLytter(
    rapidsConnection: RapidsConnection,
    private val esClient: ESClient
) :
    River.PacketListener {

    private val secureLog = SecureLog(log)

    init {
        River(rapidsConnection).apply {
            precondition{
                it.requireKey("aktørId")
                it.requireValue("synlighet.erSynlig", false)
                it.requireValue("synlighet.ferdigBeregnet", true)
                it.forbidValue("@slutt_av_hendelseskjede", true)
            }
        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry
    ) {
        esClient.slettCv(packet["aktørId"].asText())
        packet["@slutt_av_hendelseskjede"] = true
        context.publish(packet.toJson())
    }
}
