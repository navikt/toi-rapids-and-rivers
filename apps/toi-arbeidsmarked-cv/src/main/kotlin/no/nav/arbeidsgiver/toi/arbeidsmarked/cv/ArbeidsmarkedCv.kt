package no.nav.arbeidsgiver.toi.arbeidsmarked.cv

import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import no.nav.arbeid.cv.avro.Melding

class ArbeidsmarkedCv(melding: Melding) {
    @JsonProperty("@event_name")
    private val event_name = "arbeidsmarked-cv"

    val aktørId = melding.aktoerId
    val arbeidsmarkedCv = melding

    fun somJson() = JsonMessage(objectMapper.writeValueAsString(this), MessageProblems("{}")).toJson()

}

private val objectMapper = JsonMapper.builder()
    .addModule(kotlinModule())
    .addMixIn(Object::class.java, AvroMixIn::class.java)
    .build()

abstract class AvroMixIn {
    @JsonIgnore
    abstract fun getSchema(): org.apache.avro.Schema

    @JsonIgnore
    abstract fun getSpecificData(): org.apache.avro.specific.SpecificData
}
