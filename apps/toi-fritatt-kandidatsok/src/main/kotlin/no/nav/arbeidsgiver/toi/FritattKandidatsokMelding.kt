package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.arbeid.cv.events.CvEvent
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime


fun CvEvent.tilFritattKandidatsokMelding() = mapOf(
    "@event_name" to "fritatt-kandidatsøk",
    "fodselsnummer" to fodselsnummer,
    "fritattKandidatsøk" to mapOf(
        "fritattKandidatsok" to fritattKandidatsok
    )
).let(objectMapper::writeValueAsString)

data class FritattKandidatsok(
    val fødselsnummer: String,
    val fritattKandidatsøk: Boolean,
    val sistEndretTidspunkt: ZonedDateTime,
    val sistEndretAvSystem: String,
    val sistEndretAvVeileder: String?,
)

fun CvEvent.tilFritattKandidatsok() =
    FritattKandidatsok(
        fødselsnummer = fodselsnummer,
        fritattKandidatsøk = fritattKandidatsok,
        sistEndretTidspunkt = ZonedDateTime.of(LocalDateTime.parse(tidsstempel), ZoneId.of("Europe/Oslo")),
        sistEndretAvSystem = "Arena",
        sistEndretAvVeileder = null,
    )

private val objectMapper = ObjectMapper()
    .addMixIn(Object::class.java, AvroMixIn::class.java)

abstract class AvroMixIn {
    @JsonIgnore
    abstract fun getSchema(): org.apache.avro.Schema

    @JsonIgnore
    abstract fun getSpecificData(): org.apache.avro.specific.SpecificData
}
