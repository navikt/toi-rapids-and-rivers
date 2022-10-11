package no.nav.arbeidsgiver.toi.arbeidsmarked.cv

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import no.nav.arbeid.cv.avro.Cv
import no.nav.arbeid.cv.avro.Melding
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems

class CvMelding(cv: Cv) {
    @JsonProperty("@event_name")
    private val event_name = "arbeidsmarked-cv"

    val aktørId = cv.aktoerId
    val cvMelding = cv

    fun somJson()= JsonMessage(objectMapper.writeValueAsString(this), MessageProblems("{}")).toJson()

}

private val objectMapper = ObjectMapper()
    .registerModule(JavaTimeModule())
    .addMixIn(Object::class.java, AvroMixIn::class.java)

abstract class AvroMixIn {
    @JsonIgnore
    abstract fun getSchema(): org.apache.avro.Schema
    @JsonIgnore
    abstract fun getSpecificData() : org.apache.avro.specific.SpecificData
}
