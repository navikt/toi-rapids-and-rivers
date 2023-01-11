package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.arbeid.cv.events.CvEvent
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class FritattKandidatsokMelding(melding: CvEvent) {
    @JsonProperty("@event_name")
    private val event_name = "fritatt-kandidatsøk"

    val fodselsnummer = melding.fodselsnummer
    val fritattKandidatsøk = FritattKandidatsok(melding.fritattKandidatsok)

    fun somString() = objectMapper.writeValueAsString(this)
}

data class FritattKandidatsokIDatabase(
    val fødselsnummer: String,
    val fritattKandidatsøk: Boolean,
    val sistEndretTidspunkt: ZonedDateTime,
    val sistEndretAvSystem: String,
    val sistEndretAvVeileder: String?,
)

fun fritattKandidatsokTilDatabase(melding: CvEvent) =
    FritattKandidatsokIDatabase(
        fødselsnummer = melding.fodselsnummer,
        fritattKandidatsøk = melding.fritattKandidatsok,
        sistEndretTidspunkt = ZonedDateTime.of(LocalDateTime.parse(melding.tidsstempel), ZoneId.of("Europe/Oslo")),
        sistEndretAvSystem = "Arena",
        sistEndretAvVeileder = null,
    )

data class FritattKandidatsok(
    val fritattKandidatsok: Boolean,
)

private val objectMapper = ObjectMapper()
    .addMixIn(Object::class.java, AvroMixIn::class.java)

abstract class AvroMixIn {
    @JsonIgnore
    abstract fun getSchema(): org.apache.avro.Schema

    @JsonIgnore
    abstract fun getSpecificData(): org.apache.avro.specific.SpecificData
}
