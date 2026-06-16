package no.nav.arbeidsgiver.toi.kvp

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry


class KvpLytter(private val rapidsConnection: RapidsConnection) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            precondition{
                it.requireKey("event")
                it.requireKey("aktorId")
                it.requireKey("startet")
                it.interestedIn("avsluttet")
                it.forbid("@event_name")
            }
            validate {
                it.requireKey("enhetId")
            }
        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry
    ) {
        log.info("Mottok kvp event ${packet["event"].asString()}")

        if (packet["event"].isNull || (packet["event"].asString() != "STARTET" && packet["event"].asString() != "AVSLUTTET")) {
            log.error("event er ikke startet eller avluttet, se secure-log")
            return
        }


        val aktørId = packet["aktorId"].asString()
        val melding = mapOf(
            "aktørId" to aktørId,
            "kvp" to packet.fjernMetadataOgKonverter(),
            "@event_name" to "kvp",
        )

        secureLog.info("Skal publisere kvp-melding med event ${packet["event"].asString()} (securelog verifikasjon)")

        val nyPacket = JsonMessage.newMessage(melding)
        rapidsConnection.publish(aktørId, nyPacket.toJson())
    }


    override fun onError(problems: MessageProblems, context: MessageContext, metadata: MessageMetadata) {
        log.error("noe mangler i kvp.melding, se secure-log")
    }
}
