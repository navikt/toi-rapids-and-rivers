package no.nav.arbeidsgiver.toi.kandidatfeed

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.*

class UferdigKandidatLytter(
    rapidsConnection: RapidsConnection
) :
    River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandKey("aktørId")
                it.demandValue("synlighet.erSynlig", true)
                it.demandValue("synlighet.ferdigBeregnet", true)
                it.requireKey("oppfølgingsinformasjon.oppfolgingsenhet")
                it.interestedIn("@behov")
                it.interestedIn("arbeidsmarkedCv.opprettCv", "arbeidsmarkedCv.endreCv", "arbeidsmarkedCv.opprettJobbprofil", "arbeidsmarkedCv.endreJobbprofil")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        if (packet.rejectOnAll("@behov", behovsListe)) return

        val aktørId = packet["aktørId"].asText()
        packet["@behov"] = packet["@behov"].toSet() + behovsListe

        leggTilOntologiBehovFelt(packet)

        log.info("Sender behov for $aktørId")
        context.publish(aktørId, packet.toJson())
    }

    private fun leggTilOntologiBehovFelt(packet: JsonMessage) {
        val cvMelding =
            if (packet["arbeidsmarkedCv.opprettCv"].jsonNodeHasValue()) packet["arbeidsmarkedCv.opprettCv"]
            else if (packet["arbeidsmarkedCv.endreCv"].jsonNodeHasValue()) packet["arbeidsmarkedCv.endreCv"]
            else throw RuntimeException("Cv må finnes i UferdigKandidatLytter")

        val jobbMelding =
            if (packet["arbeidsmarkedCv.opprettJobbprofil"].jsonNodeHasValue()) packet["arbeidsmarkedCv.opprettJobbprofil"]
            else if (packet["arbeidsmarkedCv.endreJobbprofil"].jsonNodeHasValue()) packet["arbeidsmarkedCv.endreJobbprofil"]
            else throw RuntimeException("Jobbprofil må finnes i UferdigKandidatLytter")

        packet["kompetanse"] = jobbMelding["jobbprofil"]["kompetanser"].map(JsonNode::asText)

        val jobbønskeListe = jobbMelding["jobbprofil"]["stillinger"].map(JsonNode::asText)
        val jobbønskeKladdeListe = jobbMelding["jobbprofil"]["stillingkladder"].map(JsonNode::asText)
        val arbeidserfaringsListe = cvMelding["cv"]["arbeidserfaring"].toList().map { it["stillingstittel"].asText() }
        packet["stillingstittel"] = arbeidserfaringsListe.union(jobbønskeListe).union(jobbønskeKladdeListe)
    }


    override fun onError(problems: MessageProblems, context: MessageContext) {
        log.error(problems.toString())
    }

}

private fun JsonNode.jsonNodeHasValue() = !this.isMissingOrNull()

private fun JsonMessage.rejectOnAll(key: String, values: List<String>) = get(key).let { node ->
    !node.isMissingNode && node.isArray && node.map(JsonNode::asText).containsAll(values)
}
