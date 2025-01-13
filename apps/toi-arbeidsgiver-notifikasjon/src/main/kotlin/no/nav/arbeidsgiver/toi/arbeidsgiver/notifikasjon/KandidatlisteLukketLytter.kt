package no.nav.arbeidsgiver.toi.arbeidsgiver.notifikasjon

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import java.util.*

class KandidatlisteLukketLytter(
    rapidsConnection: RapidsConnection,
    private val notifikasjonKlient: NotifikasjonKlient
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            precondition{
                it.requireValue("@event_name", "kandidat_v2.LukketKandidatliste")
                it.forbidValue("@slutt_av_hendelseskjede", true)
            }
            validate {
                it.requireKey("stillingsId")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext, metadata: MessageMetadata, meterRegistry: MeterRegistry) {
        val stillingsId = UUID.fromString(packet["stillingsId"].asText())

        notifikasjonKlient.ferdigstillSak(stillingsId)
    }

    override fun onError(problems: MessageProblems, context: MessageContext, metadata: MessageMetadata) {
        log.error("Feil ved lesing av hendelse kandidat_v2.LukketKandidatliste: $problems")
        super.onError(problems, context, metadata)
    }
}