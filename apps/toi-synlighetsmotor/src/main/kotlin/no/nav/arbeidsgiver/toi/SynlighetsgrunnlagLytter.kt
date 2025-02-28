package no.nav.arbeidsgiver.toi

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry

class SynlighetsgrunnlagLytter(
    private val rapidsConnection: RapidsConnection,
    private val repository: Repository
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

    }
}


private fun JsonMessage.requireAny(keys: List<String>) {
    if(keys.onEach { interestedIn(it) }
            .all { this[it].isMissingNode })
        throw MessageProblems.MessageException(MessageProblems(toJson()).apply { this.error("Ingen av feltene fantes i meldingen") })
}

