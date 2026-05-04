package no.nav.arbeidsgiver.toi

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry

private const val synlighetBehov = "synlighet"

/**
 * Lytter som besvarer synlighet-behov.
 *
 * Følger need-patternet:
 * - Lytter på uløst synlighet-behov
 * - Trigger need-melding på manglende felter for å beregne synlighet
 * - Overlater ansvaret for å besvare resten av kjeden til SynlighetsgrunnlagLytter
 *
 * Lytter legges til for å prøve å unngå problem der data kan mistes av at flere kilder på samme data publiseres fra på samme tidspunkt ved reindeksering.
 * ( https://trello.com/c/q1jCdy07/440-cv-er-ute-av-synk )
 */
class SynlighetBehovsLytter(
    private val rapidsConnection: RapidsConnection,
) : River.PacketListener {

    init {
        River(rapidsConnection).apply {
            precondition {
                it.demandAtFørstkommendeUløsteBehovEr(synlighetBehov)
                it.forbid(*requiredFieldsSynlighetsbehovUntattadressebeskyttelse().toTypedArray())
            }
            validate {
                it.requireKey("aktørId")
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

        val behov = packet["@behov"]
        packet["@behov"] = requiredFieldsSynlighetsbehovUntattadressebeskyttelse() + behov
        rapidsConnection.publish(aktørId, packet.toJson())
    }

    override fun onError(problems: MessageProblems, context: MessageContext, metadata: MessageMetadata) {
        log.error("Feil ved prosessering av synlighet-behov: $problems")
    }
}
