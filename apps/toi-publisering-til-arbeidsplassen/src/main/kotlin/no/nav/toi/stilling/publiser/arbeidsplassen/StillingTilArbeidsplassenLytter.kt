package no.nav.toi.stilling.publiser.arbeidsplassen

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import no.nav.toi.stilling.publiser.arbeidsplassen.DirektemeldtStilling.Companion.fraJson

class StillingTilArbeidsplassenLytter(rapidsConnection: RapidsConnection) : River.PacketListener  {
    init {
        River(rapidsConnection).apply {
            precondition {
                it.requireKey("direktemeldtStilling")
                it.requireKey("stillingsId")
                it.requireValue("@event_name", "publiserTilArbeidsplassen")
            }
        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry
    ) {
        val stilling = fraJson(packet)

        log.info("Mottok stilling med stillingsId ${stilling.stillingsId}")

    }
}
