package no.nav.arbeidsgiver.toi.arbeidssoekerperiode

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.micrometer.core.instrument.MeterRegistry
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

class ArbeidssokerPeriode(@JsonIgnore private val melding: Periode,
                          @JsonIgnore val meterRegistry: MeterRegistry) {
    companion object {
        @JsonIgnore
        private val objectMapper: ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .setTimeZone(TimeZone.getTimeZone("Europe/Oslo"))
    }

    //@JsonProperty("@event_name")
    //private val event_name = "arbeidssokerperiode"

    // ID er meldingsid. Denne id'en for å koble brukeren mot ArbeidssoekerOpplysninger
    // Kan også brukes til å koble sammen opplysninger om arbeidssøkeren som kommer inn på andre topic
    @JsonProperty("periode_id")
    val periodeId: UUID = melding.id
    // FNR/DNR - merk at hvis noen endrer dnr/fnr, så kommer det en ny melding på nytt nummer.
    val identitetsnummer: String = melding.identitetsnummer
    val startet: ZonedDateTime = melding.startet.tidspunkt.atZone(ZoneId.of("Europe/Oslo"))
    val avsluttet: ZonedDateTime? = melding.avsluttet?.tidspunkt?.atZone(ZoneId.of("Europe/Oslo"))


    fun somJsonNode() =
        objectMapper.valueToTree<JsonNode>(this)
}
