package no.nav.toi.stilling.indekser

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry

class DirektemeldtStillingLytter(rapidsConnection: RapidsConnection) : River.PacketListener {

    init {
        River(rapidsConnection).apply {
            precondition {
                it.requireKey("stillingsinfo")
                it.requireKey("direktemeldtStilling")
                it.requireValue("@event_name", "DirektemeldtStillingIndekser")
            }
            validate { it.requireKey("stillingsId") }
        }.register(this)
    }


    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry
    ) {
        log.info("Mottok melding for indeksering: $packet")
    }
}
