package no.nav.arbeidsgiver.toi.kandidat.indekser.domene

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import java.time.OffsetDateTime
import java.util.Objects

@JsonIgnoreProperties(ignoreUnknown = true)
class EsSprak(
    @field:JsonProperty private val fraDato: OffsetDateTime?,
    @field:JsonProperty private val sprakKode: String?,
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @field:JsonProperty private val sprakKodeTekst: String,
    @field:JsonProperty private val alternativTekst: String,
    @field:JsonProperty private val beskrivelse: String,
    @field:JsonProperty private val ferdighetMuntlig: String?,
    @field:JsonProperty private val ferdighetSkriftlig: String?
) {
    constructor(fraDato: OffsetDateTime?, sprakKode: String?, sprakKodeTekst: String, alternativTekst: String, beskrivelse: String): this(
        fraDato,
        sprakKode,
        sprakKodeTekst,
        alternativTekst,
        beskrivelse,
        null,
        null
    )
    constructor(spraaknavn: String, ferdighetMuntlig: String?, ferdighetSkriftlig: String?): this(
        null,
        null,
        spraaknavn,
        spraaknavn,
        listOfNotNull(
            if (ferdighetMuntlig?.let { it.isNotBlank() } == true) "Muntlig: $ferdighetMuntlig" else null,
            if (ferdighetSkriftlig?.let { it.isNotBlank() } == true) "Skriftlig: $ferdighetSkriftlig" else null
        ).joinToString(" "),
        ferdighetMuntlig,
        ferdighetSkriftlig
    )

    override fun equals(other: Any?) = other is EsSprak && fraDato == other.fraDato && sprakKode == other.sprakKode
            && sprakKodeTekst == other.sprakKodeTekst
            && alternativTekst == other.alternativTekst
            && beskrivelse == other.beskrivelse
            && ferdighetMuntlig == other.ferdighetMuntlig
            && ferdighetSkriftlig == other.ferdighetSkriftlig

    override fun hashCode() = Objects.hash(
        fraDato,
        sprakKode,
        sprakKodeTekst,
        alternativTekst,
        beskrivelse,
        ferdighetMuntlig,
        ferdighetSkriftlig
    )

    override fun toString() = ("EsSprak{" + "fraDato=" + fraDato + ", sprakKode='" + sprakKode + '\''
            + ", sprakKodeTekst='" + sprakKodeTekst + '\'' + ", alternativTekst='" + alternativTekst
            + '\'' + ", beskrivelse='" + beskrivelse + '\''
            + ", ferdighetMuntlig='" + ferdighetMuntlig + '\'' + ", ferdighetSkriftlig='" + ferdighetSkriftlig + '\'' + '}')

    companion object {
        fun fraMelding(cvNode: JsonNode) = cvNode["spraakferdigheter"].map { spraakferdigheter ->
            EsSprak(
                spraaknavn = spraakferdigheter["spraaknavn"].asText(),
                ferdighetMuntlig = spraakferdigheter["ferdighetMuntlig"]?.asText(null),
                ferdighetSkriftlig = spraakferdigheter["ferdighetSkriftlig"]?.asText(null)
            )
        }
    }
}
