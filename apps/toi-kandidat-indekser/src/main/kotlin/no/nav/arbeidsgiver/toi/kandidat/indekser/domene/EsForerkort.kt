package no.nav.arbeidsgiver.toi.kandidat.indekser.domene

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.Objects

@JsonIgnoreProperties(ignoreUnknown = true)
class EsForerkort(
    private val fraDato: LocalDate?,
    private val tilDato: LocalDate?,
    private val forerkortKode: String?,
    @JsonInclude(JsonInclude.Include.NON_EMPTY) private val forerkortKodeKlasse: String,
    private val alternativKlasse: String?,
    private val utsteder: String?
) {
    constructor(fraDato: LocalDate?, tilDato: LocalDate?, klasse: String, klassebeskrivelse: String) : this(
        fraDato,
        tilDato,
        null,  // Det finnes to formater på førerkort, så vi må håndtere begge
        if (klasse.contains("-")) klasse else klasse + " - " + klassebeskrivelse,
        null,
        null
    )

    override fun equals(other: Any?) = other is EsForerkort && fraDato == other.fraDato && tilDato == other.tilDato
            && forerkortKode == other.forerkortKode
            && forerkortKodeKlasse == other.forerkortKodeKlasse
            && alternativKlasse == other.alternativKlasse
            && utsteder == other.utsteder

    override fun hashCode() = Objects.hash(
        fraDato, tilDato, forerkortKode, forerkortKodeKlasse, alternativKlasse,
        utsteder
    )

    override fun toString() = ("EsForerkort{" + "fraDato=" + fraDato + ", tilDato=" + tilDato + ", forerkortKode='"
            + forerkortKode + '\'' + ", forerkortKodeKlasse='" + forerkortKodeKlasse + '\''
            + ", alternativKlasse='" + alternativKlasse + '\'' + ", utsteder='" + utsteder + '\'' + '}')

    companion object {
        fun fraMelding(cvNode: JsonNode): List<EsForerkort> {
            return cvNode["foererkort"]["klasse"].map { forerkortNode ->
                EsForerkort(
                    fraDato = forerkortNode["fraTidspunkt"]?.let { if(it.isMissingOrNull()) null else it.yyyyMMddTilLocalDate() },
                    tilDato = forerkortNode["utloeper"]?.let { if(it.isMissingOrNull()) null else it.yyyyMMddTilLocalDate() },
                    klasse = forerkortNode["klasse"].asText(null),
                    klassebeskrivelse = forerkortNode["klasseBeskrivelse"].asText(),
                )
            }
        }
    }
}
