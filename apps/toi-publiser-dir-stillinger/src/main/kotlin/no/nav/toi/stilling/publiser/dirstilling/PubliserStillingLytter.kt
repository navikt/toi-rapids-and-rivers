package no.nav.toi.stilling.publiser.dirstilling

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import no.nav.toi.stilling.publiser.dirstilling.dto.RapidHendelse
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import java.util.*

class PubliserStillingLytter(rapidsConnection: RapidsConnection,
                             private val dirStillingProducer: Producer<String, String>,
                             private val publiseringTopic: String
) : River.PacketListener  {
    init {
        River(rapidsConnection).apply {
            precondition {
                it.requireKey("direktemeldtStilling")
                it.interestedIn("stillingsinfo")
                it.requireValue("@event_name", "indekserDirektemeldtStilling")
                it.forbid("stilling") // Ikke les meldingen på nytt etter at den har vært innom stillingPopulator
            }
            validate { it.requireKey("stillingsId") }
        }.register(this)
    }

    private val objectMapper: ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .setTimeZone(TimeZone.getTimeZone("Europe/Oslo"))

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry
    ) {
        val direktemeldtStilling = RapidHendelse.fraJson(packet).direktemeldtStilling
        log.info("Mottok stilling med stillingsId ${direktemeldtStilling.stillingsId}")

        val stillingskategori = RapidHendelse.fraJson(packet).stillingsinfo
        val stilling = direktemeldtStilling.konverterTilStilling(stillingskategori?.stillingskategori)
        val melding = ProducerRecord(publiseringTopic, stilling.uuid.toString(), objectMapper.writeValueAsString(stilling))

        dirStillingProducer.send(melding) { _, exception ->
            if (exception == null) {
                log.info("Publisert stilling med stillingsId ${direktemeldtStilling.stillingsId} på topic $publiseringTopic")
            } else {
                log.error(
                    "Greide ikke å publisere stilling med stillingsId ${direktemeldtStilling.stillingsId} på topic $publiseringTopic",
                    exception
                )
            }
        }
    }
}
