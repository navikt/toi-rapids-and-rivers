package no.nav.arbeidsgiver.toi.arbeidsgiver.notifikasjon

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import java.time.ZonedDateTime
import java.util.*

class NotifikasjonLytter(rapidsConnection: RapidsConnection, private val notifikasjonKlient: NotifikasjonKlient) : River.PacketListener {

    init {
        River(rapidsConnection).apply {
            precondition{
                it.requireValue("@event_name", "notifikasjon.cv-delt")
                it.requireKey("stilling.stillingstittel")
            }
            validate {
                it.requireKey(
                    "notifikasjonsId",
                    "virksomhetsnummer",
                    "stillingsId",
                    "utførtAvVeilederFornavn",
                    "utførtAvVeilederEtternavn",
                    "arbeidsgiversEpostadresser",
                    "tidspunktForHendelse",
                    "meldingTilArbeidsgiver",
                )
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext, metadata: MessageMetadata, meterRegistry: MeterRegistry) {
        val notifikasjonsId = packet["notifikasjonsId"].asString()
        val stillingsId = UUID.fromString(packet["stillingsId"].asString())
        val virksomhetsnummer = packet["virksomhetsnummer"].asString()
        val utførtAvVeilederFornavn = packet["utførtAvVeilederFornavn"].asString()
        val utførtAvVeilederEtternavn = packet["utførtAvVeilederEtternavn"].asString()
        val arbeidsgiversEpostadresser = packet["arbeidsgiversEpostadresser"].toList().map { it.asString() }
        val tidspunktForHendelse = ZonedDateTime.parse(packet["tidspunktForHendelse"].asString())
        val meldingTilArbeidsgiver = packet["meldingTilArbeidsgiver"].asString()
        val stillingstittel = packet["stilling.stillingstittel"].asString()

        notifikasjonKlient.sendNotifikasjon(
            notifikasjonsId = notifikasjonsId,
            mottakerEpostadresser = arbeidsgiversEpostadresser,
            stillingsId = stillingsId,
            virksomhetsnummer = virksomhetsnummer,
            avsender = "$utførtAvVeilederFornavn $utførtAvVeilederEtternavn",
            tidspunktForHendelse = tidspunktForHendelse,
            meldingTilArbeidsgiver = meldingTilArbeidsgiver,
            stillingstittel = stillingstittel
        )
    }
}
