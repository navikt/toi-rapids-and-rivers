package no.nav.arbeidsgiver.toi.evaluertdatalogger

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

class EvaluertDataLytter(rapidsConnection: RapidsConnection): River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("synlighet.erSynlig", true)
                it.demandValue("synlighet.ferdigBeregnet", true)
                it.demandKey("aktørId")
                it.demandKey("oppfølgingsinformasjon.formidlingsgruppe")
                it.demandValue("@slutt_av_hendelseskjede", true)
                it.demandValue("@event_name", "republisert")
            }
        }.register(this)
    }
    private val synligeMedRettighetsGruppe = mutableMapOf<String, String>()

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val aktørId = packet["aktørId"].asText()
        val formidlingsgruppe = packet["oppfølgingsinformasjon.formidlingsgruppe"].asText()
        secureLog.info("(secure) Synlig bruker med rettighetsgruppe $formidlingsgruppe ($aktørId)")
    }
}

private val secureLog = LoggerFactory.getLogger("secureLog")