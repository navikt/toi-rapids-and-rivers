package no.nav.arbeidsgiver.toi.kandidat.indekser.domene

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.Objects

@JsonIgnoreProperties(ignoreUnknown = true)
class EsSertifikat(
    @field:JsonProperty private val fraDato: LocalDate,
    @field:JsonProperty private val tilDato: LocalDate?,
    @field:JsonProperty private val sertifikatKode: String?,
    @field:JsonProperty @JsonInclude(JsonInclude.Include.NON_EMPTY) private val sertifikatKodeNavn: String?,
    @field:JsonProperty private val alternativtNavn: String?,
    @field:JsonProperty private val utsteder: String
): EnAvFlereSamledeKompetaser {

    constructor(fraDato: LocalDate, tilDato: LocalDate?, tittel: String, utsteder: String) : this(
        fraDato,
        tilDato,
        null,
        tittel,
        tittel,
        utsteder
    )

    override fun equals(other: Any?) = other is EsSertifikat && fraDato == other.fraDato && tilDato == other.tilDato
            && sertifikatKode == other.sertifikatKode
            && sertifikatKodeNavn == other.sertifikatKodeNavn
            && alternativtNavn == other.alternativtNavn
            && utsteder == other.utsteder

    override fun hashCode() = Objects.hash(
        fraDato, tilDato, sertifikatKode, sertifikatKodeNavn, alternativtNavn,
        utsteder
    )

    override fun toString() = ("EsSertifikat{" + "fraDato=" + fraDato + ", tilDato=" + tilDato + ", sertifikatKode='"
            + sertifikatKode + '\'' + ", sertifikatKodeNavn='" + sertifikatKodeNavn + '\''
            + ", alternativtNavn='" + alternativtNavn + '\'' + ", utsteder='" + utsteder + '\'' + '}')

    override fun tilSamletKompetanse() = sertifikatKodeNavn?.let { listOf(EsSamletKompetanse(it)) } ?: emptyList()

    companion object {
        fun fraMelding(cvNode: JsonNode) = cvNode["sertifikat"].map { sertifikatNode ->
            EsSertifikat(
                fraDato = sertifikatNode["gjennomfoert"].yyyyMMddTilLocalDate(),
                tilDato = sertifikatNode["utloeper"]?.let(JsonNode::yyyyMMddTilLocalDate),
                sertifikatKode = sertifikatNode["konseptId"].asText(null),
                sertifikatKodeNavn = sertifikatNode["sertifikatnavn"].asText(null),
                alternativtNavn = sertifikatNode["sertifikatnavnFritekst"].asText(null),
                utsteder = sertifikatNode["utsteder"].asText(),
            )
        }
    }
}
