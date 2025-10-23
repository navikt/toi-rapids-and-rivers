package no.nav.arbeidsgiver.toi.kandidat.indekser

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsCv
import no.nav.arbeidsgiver.toi.kandidat.indekser.geografi.GeografiKlient
import no.nav.arbeidsgiver.toi.kandidat.indekser.geografi.PostDataKlient

const val topicName = "toi.kandidat-3"

class SynligKandidatfeedLytter(
    rapidsConnection: RapidsConnection,
    private val esClient: ESClient,
    private val postDataKlient: PostDataKlient,
    private val geografiKlient: GeografiKlient,
) :
    River.PacketListener {

    private val secureLog = SecureLog(log)

    init {
        River(rapidsConnection).apply {
            precondition {
                it.requireKey("aktørId")
                it.requireValue("synlighet.erSynlig", true)
                it.requireValue("synlighet.ferdigBeregnet", true)
                it.forbidValue("@slutt_av_hendelseskjede", true)
                behovsListe.forEach(it::requireKey)
            }
            validate {
                it.requireKey("oppfølgingsinformasjon.oppfolgingsenhet", "arbeidsmarkedCv", "ontologi.stillingstittel", "ontologi.kompetansenavn", "hullICv.sluttdatoerForInaktivePerioder")
                it.interestedIn("oppfølgingsinformasjon.kvalifiseringsgruppe", "oppfølgingsinformasjon.formidlingsgruppe", "oppfølgingsinformasjon.hovedmaal", "siste14avedtak.hovedmal", "siste14avedtak.innsatsgruppe", "fritattKandidatsøk.fritattKandidatsok", "veileder.veilederId", "veileder.veilederinformasjon.visningsNavn", "veileder.veilederinformasjon.epost", "hullICv.førsteDagIInneværendeInaktivePeriode")
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

        esClient.lagreEsCv(EsCv.fraMelding(packet, postDataKlient, geografiKlient))
        packet["@slutt_av_hendelseskjede"] = true
        context.publish(packet.toJson())
    }

    override fun onError(problems: MessageProblems, context: MessageContext, metadata: MessageMetadata) {
        log.error(problems.toString())
        throw Error()
    }
}