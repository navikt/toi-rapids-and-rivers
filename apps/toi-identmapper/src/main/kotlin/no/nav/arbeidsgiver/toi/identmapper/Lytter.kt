package no.nav.arbeidsgiver.toi.identmapper

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

class Lytter(
    private val fnrKey: String,
    private val rapidsConnection: RapidsConnection,
    private val cluster: String,
    private val hentAktørId: (fødselsnummer: String) -> String?,
) : River.PacketListener {
    private val aktørIdKey = "aktørId"

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandKey(fnrKey)
                it.rejectKey(aktørIdKey, "aktorId", "aktoerId")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val aktørId = hentAktørId(packet[fnrKey].asText())

        if (aktørId == null) {
            if (cluster == "prod-gcp") {
                val identtype = if (erDnr(packet[fnrKey].asText())) "D-nummer" else "fødselsnummer"
                log.info("Fant ikke gitt person i PDL, klarte ikke å mappe $identtype til aktørId")
            }
        } else {
            log.info("Mappet fra fødselsnummer til aktørId: $aktørId")
            packet[aktørIdKey] = aktørId
            rapidsConnection.publish(packet.toJson())
        }
    }
}

fun erDnr(s: String) = s[0].digitToInt() > 3