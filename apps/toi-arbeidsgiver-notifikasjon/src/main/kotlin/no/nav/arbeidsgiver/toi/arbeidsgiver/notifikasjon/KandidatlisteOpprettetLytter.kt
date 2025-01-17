package no.nav.arbeidsgiver.toi.arbeidsgiver.notifikasjon

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import java.util.*

class KandidatlisteOpprettetLytter(
    rapidsConnection: RapidsConnection,
    private val notifikasjonKlient: NotifikasjonKlient
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            precondition() {
                it.requireValue("@event_name", "kandidat_v2.OpprettetKandidatliste")
                it.requireKey("stillingsId")
                it.requireKey("stilling.stillingstittel")
                it.requireKey("stilling.organisasjonsnummer")
                it.requireValue("stillingsinfo.stillingskategori", "STILLING")
                it.forbidValue("@slutt_av_hendelseskjede", true)
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext, metadata: MessageMetadata, meterRegistry: MeterRegistry) {
        val stillingsId = UUID.fromString(packet["stillingsId"].asText())
        val stillingstittel = packet["stilling.stillingstittel"].asText()
        val organisasjonsnummer = packet["stilling.organisasjonsnummer"].asText()

        notifikasjonKlient.opprettSak(
            stillingsId = stillingsId,
            stillingstittel = stillingstittel,
            organisasjonsnummer = organisasjonsnummer,
        )
    }
}
