package no.nav.arbeidsgiver.toi.identmapper

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import no.nav.arbeidsgiver.toi.logging.TeamLogLogger.Companion.teamlog
import no.nav.arbeidsgiver.toi.logging.log

class AktørIdPopulator(
    private val fnrKey: String,
    private val rapidsConnection: RapidsConnection,
    private val cluster: String,
    private val hentAktørId: (fødselsnummer: String) -> String?,
) : River.PacketListener {
    private val aktørIdKey = "aktørId"

    private val teamlog = teamlog(log)

    init {
        River(rapidsConnection).apply {
            precondition {
                it.requireKey(fnrKey)
                it.forbid(aktørIdKey, "aktorId", "aktoerId")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext, metadata: MessageMetadata, meterRegistry: MeterRegistry) {
        val aktørId = hentAktørId(packet[fnrKey].asString())

        if (aktørId == null) {
            if (cluster == "prod-gcp") {
                val identtype = if (erDnr(packet[fnrKey].asString())) "D-nummer" else "fødselsnummer"
                log.info("Fant ikke gitt person i PDL, klarte ikke å mappe identtype (se teamlog) til aktørId")
                teamlog.info("Fant ikke gitt person i PDL, klarte ikke å mappe $identtype til aktørId")
            }
        } else {
            log.info("Publiserer melding som har fødselsnummer ($fnrKey), som manglet aktørid")
            packet[aktørIdKey] = aktørId
            rapidsConnection.publish(aktørId, packet.toJson())
        }
    }
}

fun erDnr(s: String) = s[0].digitToInt() > 3
