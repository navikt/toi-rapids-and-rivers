package no.nav.arbeidsgiver.toi.kandidat.indekser.domene

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import java.time.OffsetDateTime
import java.util.Objects

@JsonIgnoreProperties(ignoreUnknown = true)
class EsSertifikat(
    private val fraDato: OffsetDateTime,
    private val tilDato: OffsetDateTime?,
    private val sertifikatKode: String?,
    @JsonInclude(JsonInclude.Include.NON_EMPTY) private val sertifikatKodeNavn: String,
    private val alternativtNavn: String?,
    private val utsteder: String
): EnAvFlereSamledeKompetaser {

    constructor(fraDato: OffsetDateTime, tilDato: OffsetDateTime, tittel: String, utsteder: String) : this(
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

    override fun tilSamletKompetanse() = listOf(EsSamletKompetanse(sertifikatKodeNavn))
}
