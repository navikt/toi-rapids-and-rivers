package no.nav.toi.stilling.indekser

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import no.nav.toi.stilling.indekser.dto.KandidatlisteInfo
import java.util.UUID

class KandidatlisteInfoLytter(rapidsConnection: RapidsConnection,
                              private val openSearchService: OpenSearchService,
                              private val indeks: String
) : River.PacketListener {

    init {
        River(rapidsConnection).apply {
            precondition {
                it.requireKey("kandidatlisteInfo")
                it.requireAny("@event_name", listOf("indekserKandidatlisteInfo", "kandidatlisteInfoBehov"))
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
        val kandidatlisteInfoPacket = packet["kandidatlisteInfo"]
        val stillingsIdPacket = packet["stillingsId"]

        val kandidatlisteInfo : KandidatlisteInfo
        val stillingsId: UUID

        try {
            kandidatlisteInfo = JacksonConfig.objectMapper.readValue(kandidatlisteInfoPacket.toString(), KandidatlisteInfo::class.java)
            stillingsId = JacksonConfig.objectMapper.readValue(stillingsIdPacket.toString(), UUID::class.java)
        } catch (e: Exception) {
            log.error("Gå forbi feil format på melding", e)
            return
        }

        log.info("Mottok kandidatlisteInfo for stilling: $stillingsId")
        openSearchService.oppdaterKandidatlisteInfo(stillingsId = stillingsId.toString(), kandidatlisteInfo = kandidatlisteInfo, indeks = indeks)
    }
}