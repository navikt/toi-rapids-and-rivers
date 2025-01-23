import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import no.nav.arbeidsgiver.toi.livshendelser.DiskresjonsHendelse
import no.nav.arbeidsgiver.toi.livshendelser.PdlKlient
import no.nav.arbeidsgiver.toi.livshendelser.PersonhendelseService
import no.nav.arbeidsgiver.toi.livshendelser.log
import no.nav.person.pdl.leesah.adressebeskyttelse.Gradering
import org.slf4j.LoggerFactory

class AdressebeskyttelseLytter(private val pdlKlient: PdlKlient, private val rapidsConnection: RapidsConnection) :
    River.PacketListener {

    private val secureLog = LoggerFactory.getLogger("secureLog")

    init {
        River(rapidsConnection).apply {
            precondition{
                it.demandAtFørstkommendeUløsteBehovEr("adresseBeskyttelse")
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
        //Muklige Koder:  "STRENGT_FORTROLIG_UTLAND", "STRENGT_FORTROLIG", "FORTROLIG", "UGRADERT", null
        val aktørid: String = packet["aktørId"].asText()

        val personhendelseService = PersonhendelseService(rapidsConnection, pdlKlient)
        val diskresjonshendelser = personhendelseService.kallPdl(aktørid)
            packet["diskresjon"] = harDiskresjon(diskresjonshendelser)

        log.info("Sender løsning på behov for aktørid: (se securelog)")
        secureLog.info("Sender løsning på behov for aktørid: (se securelog) TODO: verifiser at securelog logger riktig")

        context.publish(aktørid, packet.toJson())
    }

    override fun onError(problems: MessageProblems, context: MessageContext, metadata: MessageMetadata) {
        log.error(problems.toString())
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

fun harDiskresjon(diskresjonHendelser : List<DiskresjonsHendelse>) : Boolean =
        diskresjonHendelser.any{harDiskresjon(it)}

fun harDiskresjon(diskresjonHendelse : DiskresjonsHendelse) : Boolean {
    val fortroligeKoder = setOf(
        "STRENGT_FORTROLIG_UTLAND",
        "STRENGT_FORTROLIG",
        "FORTROLIG"
    )
    return  fortroligeKoder.contains(diskresjonHendelse.gradering.toString())
}


