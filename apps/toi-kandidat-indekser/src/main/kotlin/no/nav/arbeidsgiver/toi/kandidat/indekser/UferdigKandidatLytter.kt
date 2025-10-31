package no.nav.arbeidsgiver.toi.kandidat.indekser

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import kotlin.text.get

class UferdigKandidatLytter(
    rapidsConnection: RapidsConnection
) :
    River.PacketListener {

    private val secureLog = SecureLog(log)

    init {
        River(rapidsConnection).apply {
            precondition {
                it.requireKey("aktørId")
                it.requireValue("synlighet.erSynlig", true)
                it.requireValue("synlighet.ferdigBeregnet", true)
                it.interestedIn ("@behov")
                it.interestedIn(
                    "arbeidsmarkedCv.opprettCv",
                    "arbeidsmarkedCv.endreCv",
                    "arbeidsmarkedCv.opprettJobbprofil",
                    "arbeidsmarkedCv.endreJobbprofil"
                )
            }
            validate {
                it.requireKey("oppfølgingsinformasjon.oppfolgingsenhet")

            }
        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry
    ) {
        if (packet.rejectOnAll("@behov", behovsListe)) return

        val aktørId = packet["aktørId"].asText()
        packet["@behov"] = packet["@behov"].toSet() + behovsListe
        val cvMelding =
            if (packet["arbeidsmarkedCv.opprettCv"].jsonNodeHasValue()) packet["arbeidsmarkedCv.opprettCv"]
            else if (packet["arbeidsmarkedCv.endreCv"].jsonNodeHasValue()) packet["arbeidsmarkedCv.endreCv"]
            else throw RuntimeException("Cv må finnes i UferdigKandidatLytter")
        val jobbMelding =
            if (packet["arbeidsmarkedCv.opprettJobbprofil"].jsonNodeHasValue()) packet["arbeidsmarkedCv.opprettJobbprofil"]
            else if (packet["arbeidsmarkedCv.endreJobbprofil"].jsonNodeHasValue()) packet["arbeidsmarkedCv.endreJobbprofil"]
            else throw RuntimeException("Jobbprofil må finnes i UferdigKandidatLytter")

        leggTilOntologiBehovFelt(packet, cvMelding, jobbMelding)
        leggTilGeografiBehovFelter(packet, cvMelding, jobbMelding)

        log.info("Sender behov for aktørid (se securelog)")
        secureLog.info("Sender behov for $aktørId")
        context.publish(aktørId, packet.toJson())
    }

    private fun leggTilOntologiBehovFelt(packet: JsonMessage, cvMelding: JsonNode, jobbMelding: JsonNode) {
        packet["kompetanse"] = jobbMelding["jobbprofil"]["kompetanser"].map(JsonNode::asText)

        val jobbønskeListe = jobbMelding["jobbprofil"]["stillinger"].map(JsonNode::asText)
        val jobbønskeKladdeListe = jobbMelding["jobbprofil"]["stillingkladder"].map(JsonNode::asText)
        val arbeidserfaringsListe = cvMelding["cv"]["arbeidserfaring"].toList().map { it["stillingstittel"].asText() }
        packet["stillingstittel"] = arbeidserfaringsListe.union(jobbønskeListe).union(jobbønskeKladdeListe)
    }

    private fun leggTilGeografiBehovFelter(packet: JsonMessage, cvMelding: JsonNode, jobbMelding: JsonNode) {
        packet["postnummer"] = cvMelding["cv"]["postnummer"].asText()
        packet["geografiKode"] = jobbMelding["jobbprofil"]["geografi"]?.map { it["kode"].asText() } ?: emptyList<String>()
    }

    override fun onError(problems: MessageProblems, context: MessageContext, metadata: MessageMetadata) {
        log.error(problems.toString())
    }

}

private fun JsonNode.jsonNodeHasValue() = !this.isMissingOrNull()

private fun JsonMessage.rejectOnAll(key: String, values: List<String>) = get(key).let { node ->
    !node.isMissingNode && node.isArray && node.map(JsonNode::asText).containsAll(values)
}
