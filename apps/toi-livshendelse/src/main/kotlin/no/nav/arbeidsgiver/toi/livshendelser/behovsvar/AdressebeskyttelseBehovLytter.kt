import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import no.nav.arbeidsgiver.toi.livshendelser.PdlKlient
import no.nav.arbeidsgiver.toi.livshendelser.PersonhendelseService
import no.nav.arbeidsgiver.toi.livshendelser.log
import org.slf4j.LoggerFactory

class AdressebeskyttelseLytter(private val pdlKlient: PdlKlient, private val rapidsConnection: RapidsConnection) :
    River.PacketListener {

    private val secureLog = LoggerFactory.getLogger("secureLog")

    init {
        River(rapidsConnection).apply {
            precondition{
                it.demandAtFørstkommendeUløsteBehovEr("adressebeskyttelse")
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
        //Mulige Koder:  "STRENGT_FORTROLIG_UTLAND", "STRENGT_FORTROLIG", "FORTROLIG", "UGRADERT", null(mappes til UKJENT)
        val aktørid: String = packet["aktørId"].asText()


        val personhendelseService = PersonhendelseService(rapidsConnection, pdlKlient)
        val gradering = personhendelseService.graderingFor(aktørid)
        packet["adressebeskyttelse"] = gradering ?: "UKJENT"

        // Kun til testbruk dersom vi vil skru av livshendelsesjekk på grunn av at det går for tregt, erstatter da koden ovenfor.
        //packet["adressebeskyttelse"] = "CHECK_DISABLED"

        log.info("Sender løsning på behov for aktørid: (se securelog)")
        secureLog.info("Sender løsning på behov for aktørid: $aktørid")
        if(gradering != null && gradering != "UGRADERT" ) {
            secureLog.info("Adressebeskyttelse  ${aktørid} $gradering ${packet["@event_name"].asText()}")
        }

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
                .first { this[it].isMissingNode } != informasjonsElement
        )
            throw Exception("Uinteressant hendelse")
    }
}