package no.nav.arbeidsgiver.toi.arbeidsgiver.notifikasjon

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import java.time.ZonedDateTime
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

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
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
