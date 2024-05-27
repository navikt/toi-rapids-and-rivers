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
                it.demandKey("oppfølgingsinformasjon.rettighetsgruppe")
                it.demandValue("@slutt_av_hendelseskjede", true)
            }
        }.register(this)
    }
    private val synligeMedRettighetsGruppe = mutableMapOf<String, String>()

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val aktørId = packet["aktørId"].asText()
        val rettighetsgruppe = packet["oppfølgingsinformasjon.rettighetsgruppe"].asText()
        log.info("Synlig bruker med rettighetsgruppe $rettighetsgruppe")
        secureLog.info("(secure) Synlig bruker med rettighetsgruppe $rettighetsgruppe")
    }
}

private val secureLog = LoggerFactory.getLogger("secureLog")