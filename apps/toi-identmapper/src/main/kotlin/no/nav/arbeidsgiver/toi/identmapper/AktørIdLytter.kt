package no.nav.arbeidsgiver.toi.identmapper

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry

class AktørIdLytter(
    private val rapidsConnection: RapidsConnection,
    private val hentFødselsnummer: (aktørId: String) -> String?,
) : River.PacketListener {
    private val aktørIdKey = "aktørId"
    private val fødselsnummerKey = "fodselsnummer"
    private val whitelistKey = "synlighet"

    init {
        River(rapidsConnection).apply {
            precondition {
                it.requireKey(aktørIdKey)
                it.requireKey(whitelistKey)
                it.forbid(fødselsnummerKey, "fnr", "fodselsnr")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext, metadata: MessageMetadata, meterRegistry: MeterRegistry) {
        val aktørId = packet[aktørIdKey].asText()
        val fødselsnummer = hentFødselsnummer(aktørId) ?: return

        packet[fødselsnummerKey] = fødselsnummer
        rapidsConnection.publish(aktørId, packet.toJson())
    }
}

