package no.nav.arbeidsgiver.toi.kandidat.indekser.domene

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.Objects

@JsonIgnoreProperties(ignoreUnknown = true)
class EsKurs(
    @field:JsonProperty private val tittel: String,
    @field:JsonProperty private val arrangor: String,
    @field:JsonProperty private val omfangEnhet: String?,
    @field:JsonProperty private val omfangVerdi: Int?,
    @field:JsonProperty private val tilDato: LocalDate
) {
    @field:JsonProperty private val fraDato: LocalDate? = null
    @field:JsonProperty private val beskrivelse = ""

    override fun equals(other: Any?) = other is EsKurs && tilDato == other.tilDato
            && tittel == other.tittel && arrangor == other.arrangor
            && omfangEnhet == other.omfangEnhet
            && omfangVerdi == other.omfangVerdi

    override fun hashCode() = Objects.hash(tilDato, tittel, arrangor, omfangEnhet, omfangVerdi)

    override fun toString() = ("EsKurs{" + "tilDato=" + tilDato + ", tittel='" + tittel + '\''
            + ", arrangor='" + arrangor + '\'' + ", omfangEnhet='" + omfangEnhet + '\''
            + ", omfangVerdi=" + omfangVerdi + '\'' + '}')

    companion object {
        fun fraMelding(cvNode: JsonNode) = cvNode["kurs"].map { kursNode ->
            EsKurs(
                tittel = kursNode["tittel"].asText(),
                arrangor = kursNode["utsteder"].asText(),
                omfangEnhet = kursNode["varighetEnhet"]?.asText("") ?: "",
                omfangVerdi = kursNode["varighet"]?.asInt(),
                tilDato = kursNode["tidspunkt"].yyyyMMddTilLocalDate()
            )
        }
    }
}
