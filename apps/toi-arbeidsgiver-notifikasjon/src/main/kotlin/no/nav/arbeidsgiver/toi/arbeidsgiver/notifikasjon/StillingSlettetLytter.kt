package no.nav.arbeidsgiver.toi.arbeidsgiver.notifikasjon

import no.nav.helse.rapids_rivers.*
import java.util.*

class StillingSlettetLytter(
    rapidsConnection: RapidsConnection,
    private val notifikasjonKlient: NotifikasjonKlient
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "kandidat_v2.SlettetStillingOgKandidatliste")
                it.requireKey("stillingsId")
                it.rejectValue("@slutt_av_hendelseskjede", true)
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val stillingsId = UUID.fromString(packet["stillingsId"].asText())

        notifikasjonKlient.slettSak(stillingsId)
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        log.error("Feil ved lesing av hendelse kandidat_v2.SlettetStillingOgKandidatliste: $problems")
    }
}