package no.nav.toi.stilling.indekser

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry

class IndekserStillingsinfoLytter(rapidsConnection: RapidsConnection,
                                  private val openSearchService: OpenSearchService,
                                  private val indeks: String
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            precondition {
                it.requireKey("stillingsinfo")
                it.requireValue("@event_name", "indekserStillingsinfo")
                it.forbid("stilling") // Ikke les meldingen på nytt etter at den har vært innom stillingPopulator
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
        val stillingsinfoMelding: StillingsinfoMelding
        try {
            stillingsinfoMelding = StillingsinfoMelding.fraJson(packet)
        }catch (e: Exception) {
            log.error("Gå forbi feil format på melding", e)
            return
        }
        val stillingsId = stillingsinfoMelding.stillingsId
        val stillingsinfo = stillingsinfoMelding.stillingsinfo

        if(stillingsinfo != null) {
            log.info("Mottok oppdatert stillingsinfo for stilling: $stillingsId")
            openSearchService.oppdaterStillingsinfo(stillingsId =  stillingsId, stillingsinfo = stillingsinfo, indeks = indeks)
        } else {
            log.warn("Ingen stillingsinfo i melding for stilling: $stillingsId, hopper over oppdatering av stillingsinfo")
        }
    }
}
