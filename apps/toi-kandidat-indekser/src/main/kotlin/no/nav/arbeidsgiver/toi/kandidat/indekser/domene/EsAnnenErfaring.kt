package no.nav.arbeidsgiver.toi.kandidat.indekser.domene

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth
import java.util.Objects

@JsonIgnoreProperties(ignoreUnknown = true)
class EsAnnenErfaring(
    @field:JsonProperty private val fraDato: YearMonth?,
    @field:JsonProperty private val tilDato: YearMonth?,
    @field:JsonProperty private val beskrivelse: String,
    @field:JsonProperty private val rolle: String? = null
) {
    override fun equals(other: Any?) = other is EsAnnenErfaring && fraDato == other.fraDato && tilDato == other.tilDato
            && beskrivelse == other.beskrivelse && rolle == other.rolle

    override fun hashCode() = Objects.hash(fraDato, tilDato, beskrivelse, rolle)

    override fun toString() = ("EsAnnenErfaring{" + "fraDato=" + fraDato + ", tilDato=" + tilDato + ", beskrivelse='"
            + beskrivelse + '\'' + ", rolle='" + rolle + '\'' + '}')

    companion object {
        fun fraMelding(cvNode: JsonNode) = cvNode["annenErfaring"].map { annenErfaring ->
            EsAnnenErfaring(
                fraDato = annenErfaring["fraTidspunkt"].asText(null)?.let(YearMonth::parse),
                tilDato = annenErfaring["tilTidspunkt"].asText(null)?.let(YearMonth::parse),
                beskrivelse = annenErfaring["beskrivelse"].asText(),
                rolle = annenErfaring["rolle"].asText()
            )
        }
    }
}
