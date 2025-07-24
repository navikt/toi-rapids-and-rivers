package no.nav.arbeidsgiver.toi.kandidat.indekser.domene

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
class EsSprak(
    private val fraDato: Date?,
    private val sprakKode: String?,
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private val sprakKodeTekst: String,
    private val alternativTekst: String,
    private val beskrivelse: String,
    private val ferdighetMuntlig: String?,
    private val ferdighetSkriftlig: String?
) {
    constructor(fraDato: Date?, sprakKode: String?, sprakKodeTekst: String, alternativTekst: String, beskrivelse: String): this(
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
}
