package no.nav.arbeidsgiver.toi.organisasjonsenhet

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.rapids_rivers.*
import org.slf4j.LoggerFactory

class OrganisasjonsenhetLytter(private val norg2Klient: Norg2Klient, rapidsConnection: RapidsConnection) :
    River.PacketListener {

    private val secureLog = LoggerFactory.getLogger("secureLog")

    init {
        River(rapidsConnection).apply {
            precondition{
                it.demandAtFørstkommendeUløsteBehovEr("organisasjonsenhetsnavn")
            }
            validate {
                it.requireKey("oppfølgingsinformasjon.oppfolgingsenhet")
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
        val enhetsnummer: String = packet["oppfølgingsinformasjon.oppfolgingsenhet"].asText()
        val aktørid: String = packet["aktørId"].asText()

        val orgnavn = norg2Klient.hentOrgenhetNavn(enhetsnummer)
        if (orgnavn == null) {
            if (norg2Klient.erKjentProblematiskEnhet(enhetsnummer)) {
                log.info("Mangler mapping for kjent problematisk enhet $enhetsnummer på aktørid: (se securelog), setter navn lik tom string")
                secureLog.info("Mangler mapping for kjent problematisk enhet $enhetsnummer på aktørid: $aktørid, setter navn lik tom string")
            } else {
                log.error("Mangler mapping for enhet $enhetsnummer på aktørid: (se securelog), setter navn lik tom string")
                secureLog.error("Mangler mapping for enhet $enhetsnummer på aktørid: $aktørid, setter navn lik tom string")
            }

            packet["organisasjonsenhetsnavn"] = ""
        } else {
            packet["organisasjonsenhetsnavn"] = orgnavn
        }
        log.info("Sender løsning på behov for aktørid: (se securelog) enhet: $enhetsnummer ${orgnavn ?: "''"}")
        secureLog.info("Sender løsning på behov for aktørid: $aktørid enhet: $enhetsnummer ${orgnavn ?: "''"}")

        context.publish(aktørid, packet.toJson())
    }

    override fun onError(problems: MessageProblems, context: MessageContext, metadata: MessageMetadata) {
        log.error(problems.toString())
        super.onError(problems, context, metadata)
    }
}

private fun JsonMessage.demandAtFørstkommendeUløsteBehovEr(informasjonsElement: String) {
    require("@behov") { behovNode ->
        if (behovNode
                .toList()
                .map(JsonNode::asText)
                .onEach { interestedIn(it) }
                .first { this[it].isMissingOrNull() } != informasjonsElement
        )
            throw Exception("Uinteressant hendelse")
    }
}
