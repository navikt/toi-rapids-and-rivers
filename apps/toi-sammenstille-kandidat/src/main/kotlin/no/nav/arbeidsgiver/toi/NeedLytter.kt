package no.nav.arbeidsgiver.toi

import tools.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import no.nav.arbeidsgiver.toi.logging.TeamLogLogger.Companion.teamlog
import no.nav.arbeidsgiver.toi.logging.log

class NeedLytter(
    private val rapidsConnection: RapidsConnection,
    private val repository: Repository,
    private val feltSomSkalBehandles: String
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            precondition{
                it.requireKey("aktørId")
                it.demandAtFørstkommendeUløsteBehovEr(feltSomSkalBehandles)
            }
        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry
    ) {
        try {
            log.info("Mottok need-melding")
            val aktørId = packet["aktørId"].asString()
            val kandidat = repository.hentKandidat(aktørId) ?: Kandidat(aktørId)
            kandidat.populerMelding(packet).toJson().also { rapidsConnection.publish(aktørId, it) }
            log.info("Svarte på need-melding")
        } catch (e: Exception) {
            log.error("Feil ved behandling av need-melding")
            teamlog(log).error("Feil ved behandling av need-melding: ${e.message}", e)
            throw e
        }
    }

    override fun onError(problems: MessageProblems, context: MessageContext, metadata: MessageMetadata) {
        log.error("Feil i Need-lytter: $problems")
    }
}

private fun JsonMessage.demandAtFørstkommendeUløsteBehovEr(informasjonsElement: String) {
    require("@behov") { behovNode ->
        if (behovNode
                .toList()
                .map(JsonNode::asString)
                .onEach { interestedIn(it) }
                .first { this[it].isMissingNode } != informasjonsElement
        )
            throw Exception("Uinteressant hendelse")
    }
}