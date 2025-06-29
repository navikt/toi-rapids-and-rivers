package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import no.nav.arbeidsgiver.toi.Evaluering.Companion.somSynlighet

private fun requiredFieldsSynlighetsbehovUntattadressebeskyttelse() = listOf(
    "arbeidsmarkedCv",
    "veileder",     // TODO: synlighetsmotor har ikke behov for denne. flytt need til kandidatfeed
    "oppfølgingsinformasjon",
    "siste14avedtak",     // TODO: synlighetsmotor har ikke behov for denne. flytt need til kandidatfeed
    "oppfølgingsperiode",
    "hjemmel",
    "måBehandleTidligereCv",
    "kvp",
    "arbeidssokeropplysninger"
)

private const val adressebeskyttelseFelt = "adressebeskyttelse"

class SynlighetsgrunnlagLytter(
    private val rapidsConnection: RapidsConnection,
    private val repository: Repository,
) : River.PacketListener {

    private val requiredFields = requiredFieldsSynlighetsbehovUntattadressebeskyttelse()

    init {
        River(rapidsConnection).apply {
            precondition {
                it.interestedIn("@behov")
                it.forbid("synlighet")
                it.requireAny(requiredFields + "adressebeskyttelse")
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
        val kandidat = Kandidat.fraJson(packet)

        val synlighetsevaluering = kandidat.toEvaluering()

        if (synlighetsevaluering.erFerdigBeregnet) {
            packet["synlighet"] = synlighetsevaluering.somSynlighet()
            repository.lagre(
                evaluering = synlighetsevaluering,
                aktørId = kandidat.aktørId,
                fødselsnummer = kandidat.fødselsNummer()
            )
            rapidsConnection.publish(kandidat.aktørId, packet.toJson())
        } else {
            val behov = packet["@behov"].map(JsonNode::asText)
            if (behov.containsAll(requiredFields)) {
                if (synlighetsevaluering.harAltBortsettFraAdressebeskyttelse && adressebeskyttelseFelt !in behov) {
                    val extraBehov = listOf(adressebeskyttelseFelt)
                    packet["@behov"] = (behov + extraBehov).distinct()
                    rapidsConnection.publish(kandidat.aktørId, packet.toJson())
                }
            } else {
                val extraBehov = requiredFieldsSynlighetsbehovUntattadressebeskyttelse()
                packet["@behov"] = (behov + extraBehov).distinct()
                rapidsConnection.publish(kandidat.aktørId, packet.toJson())
            }

        }
    }
}

private fun JsonMessage.requireAny(keys: List<String>) {
    if (keys.onEach { interestedIn(it) }
            .map(this::get)
            .all { it.isMissingNode }
    )
        throw MessageProblems.MessageException(MessageProblems(toJson()).apply { error("Ingen av feltene fantes i meldingen") })
}
