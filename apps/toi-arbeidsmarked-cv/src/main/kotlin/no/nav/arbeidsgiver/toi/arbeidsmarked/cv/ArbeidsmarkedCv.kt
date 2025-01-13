package no.nav.arbeidsgiver.toi.arbeidsmarked.cv

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import io.micrometer.core.instrument.MeterRegistry
import no.nav.arbeid.cv.avro.Melding

class ArbeidsmarkedCv(melding: Melding, val meterRegistry: MeterRegistry) {
    @JsonProperty("@event_name")
    private val event_name = "arbeidsmarked-cv"

    val aktørId = melding.aktoerId
    val arbeidsmarkedCv = melding

    fun somJson() = JsonMessage(objectMapper.writeValueAsString(this), MessageProblems("{}"), metrics = meterRegistry).toJson()

}

private val objectMapper = ObjectMapper()
    .registerModule(JavaTimeModule())
    .addMixIn(Object::class.java, AvroMixIn::class.java)

abstract class AvroMixIn {
    @JsonIgnore
    abstract fun getSchema(): org.apache.avro.Schema

    @JsonIgnore
    abstract fun getSpecificData(): org.apache.avro.specific.SpecificData
}
