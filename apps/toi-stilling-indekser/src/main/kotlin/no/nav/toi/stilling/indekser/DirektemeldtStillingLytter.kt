package no.nav.toi.stilling.indekser

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry

class DirektemeldtStillingLytter(rapidsConnection: RapidsConnection,
                                 private val openSearchService: OpenSearchService,
                                 private val indeks: String
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            precondition {
                it.interestedIn("stillingsinfo")
                it.requireKey("direktemeldtStilling")
                it.requireValue("@event_name", "direktemeldtStillingRepubliser")

//                it.requireKey("") // Lag en key her som sier om det er indekser eller reindekser
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
        val melding: Melding
        try {
            melding = Melding.fraJson(packet)
        }catch (e: Exception) {
            log.error("Gå forbi feil format på melding", e)
            return
        }

        // TODO lag håndtering for indeksering og reindeksering

        val direktemeldtStilling = melding.direktemeldtStilling
        val stillingsinfo = melding.stillingsinfo

        val stilling = direktemeldtStilling.tilStilling()
        val rekrutteringsbistandStilling = RekrutteringsbistandStilling(
            stilling = stilling,
            stillingsinfo = stillingsinfo
        )

        log.info("Mottok ${direktemeldtStilling.stillingsid} for indeksering: $direktemeldtStilling")

        openSearchService.indekserStilling(rekrutteringsbistandStilling, indeks)
    }
}
