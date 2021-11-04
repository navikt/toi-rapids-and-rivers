package no.nav.arbeidsgiver.toi.cv

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import no.nav.arbeid.cv.avro.Melding

class NyKandidatHendelse(melding: Melding) {
    @JsonProperty("@event_name")
    private val event_name = "Kandidat.NyFraArbeidsplassen"
    val aktørId = melding.aktoerId
    val cv = melding

    fun somString() = objectMapper.writeValueAsString(this)
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
