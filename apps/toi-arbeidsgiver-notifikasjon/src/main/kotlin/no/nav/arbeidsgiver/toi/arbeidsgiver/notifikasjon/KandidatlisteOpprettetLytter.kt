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
            validate {
                it.demandValue("@event_name", "kandidat_v2.OpprettetKandidatliste")
                it.demandKey("stillingsId")
                it.demandKey("stilling.stillingstittel")
                it.demandKey("stilling.organisasjonsnummer")
                it.demandValue("stillingsinfo.stillingskategori", "STILLING")
                it.rejectValue("@slutt_av_hendelseskjede", true)
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
