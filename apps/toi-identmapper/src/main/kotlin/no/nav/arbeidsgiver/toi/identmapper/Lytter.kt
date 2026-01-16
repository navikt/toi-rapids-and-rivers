package no.nav.arbeidsgiver.toi.identmapper

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry

class Lytter(
    private val fnrKey: String,
    private val rapidsConnection: RapidsConnection,
    private val cluster: String,
    private val hentAktørId: (fødselsnummer: String) -> String?,
) : River.PacketListener {
    private val aktørIdKey = "aktørId"

    private val secureLog = SecureLog(log)

    init {
        River(rapidsConnection).apply {
            precondition{
                it.requireKey(fnrKey)
                it.forbid(aktørIdKey, "aktorId", "aktoerId")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext, metadata: MessageMetadata, meterRegistry: MeterRegistry) {
        val aktørId = hentAktørId(packet[fnrKey].asText())

        if (aktørId == null) {
            if (cluster == "prod-gcp") {
                val identtype = if (erDnr(packet[fnrKey].asText())) "D-nummer" else "fødselsnummer"
                log.info("Fant ikke gitt person i PDL, klarte ikke å mappe identtype (se securelog) til aktørId")
                secureLog.info("Fant ikke gitt person i PDL, klarte ikke å mappe $identtype til aktørId")
            }
        } else {
            packet[aktørIdKey] = aktørId
            rapidsConnection.publish(aktørId, packet.toJson())
        }
    }
}

fun erDnr(s: String) = s[0].digitToInt() > 3