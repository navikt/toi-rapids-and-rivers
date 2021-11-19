package no.nav.arbeidsgiver.toi.kandidatfeed

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord

class KandidatfeedLytter(private val rapidsConnection: RapidsConnection, private val producer: Producer<String, String>) :
    River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandKey("aktørId")
                it.demandKey("veileder")
                it.demandKey("cv")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        if (packet["veileder"].isNull || packet["cv"].isNull) {
            val feilmelding = "cv eller veileder kan ikke være null for aktørid ${packet["aktorId"]}"
            log.error(feilmelding)
            throw IllegalArgumentException(feilmelding)
        }

        val aktørId = packet["aktørId"].asText()
        val melding = ProducerRecord("toi.kandidat-1", aktørId, packet.toJson())

        producer.send(melding) { _, exception ->
            if (exception == null) {
                log.info("Sendte kandidat med aktørId $aktørId")
            } else {
                log.error("Klarte ikke å sende kandidat med aktørId $aktørId", exception)
            }
        }
    }
}



