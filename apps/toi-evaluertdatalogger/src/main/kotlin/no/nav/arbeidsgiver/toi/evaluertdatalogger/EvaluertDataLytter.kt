package no.nav.arbeidsgiver.toi.evaluertdatalogger

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry

class EvaluertDataLytter(rapidsConnection: RapidsConnection): River.PacketListener {
    private val secureLog = SecureLog(log)

    init {
        River(rapidsConnection).apply {
            precondition{
                it.requireValue("synlighet.erSynlig", true)
                it.requireValue("synlighet.ferdigBeregnet", true)
                it.requireKey("aktørId")
                it.requireKey("oppfølgingsinformasjon.formidlingsgruppe")
                it.requireValue("@slutt_av_hendelseskjede", true)
                it.requireValue("@event_name", "republisert")
                it.interestedIn("oppfølgingsinformasjon.kvalifiseringsgruppe", "oppfølgingsinformasjon.hovedmaal","oppfølgingsinformasjon.rettighetsgruppe")
            }
        }.register(this)
    }
    private val synligeMedRettighetsGruppe = mutableMapOf<String, String>()

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry
    ) {
        val aktørId = packet["aktørId"].asString()
        val formidlingsgruppe = packet["oppfølgingsinformasjon.formidlingsgruppe"].asString()
        val kvalifiseringsgruppe = packet["oppfølgingsinformasjon.kvalifiseringsgruppe"].asString()
        val rettighetsgruppe = packet["oppfølgingsinformasjon.rettighetsgruppe"].asString()
        val hovedmaal = packet["oppfølgingsinformasjon.hovedmaal"].asString()
        secureLog.info("(secure) Synlig bruker med formidlingsgruppe $formidlingsgruppe kvalifiseringsgruppe $kvalifiseringsgruppe hovedmaal $hovedmaal rettighetsgruppe $rettighetsgruppe ($aktørId)")
    }
}