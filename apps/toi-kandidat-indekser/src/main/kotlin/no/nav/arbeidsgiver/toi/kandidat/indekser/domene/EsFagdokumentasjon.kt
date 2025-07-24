package no.nav.arbeidsgiver.toi.kandidat.indekser.domene

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
class EsFagdokumentasjon(
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private val type: String,

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private val tittel: String?,

    // TODO dette feltet er ikke lenger i bruk - bør fjernes
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private var beskrivelse: String
): EnAvFlereSamledeKompetaser {
    override fun equals(other: Any?): Boolean {
        return other is EsFagdokumentasjon && type == other.type
                && tittel == other.tittel
    }

    override fun hashCode() = Objects.hash(type, tittel)
    override fun toString() = "EsFagdokumentasjon{" + "type='" + type + '\'' + ", tittel='" + tittel + '\'' + ", beskrivelse='" + beskrivelse + '\'' + '}'
    override fun tilSamletKompetanse() = listOf(EsSamletKompetanse(tittel ?: getFagdokumentTypeLabel(type)))

    companion object {
        fun getFagdokumentTypeLabel(fagdokumentType: String) = when (fagdokumentType) {
            "SVENNEBREV_FAGBREV" -> "Fagbrev/svennebrev"
            "MESTERBREV" -> "Mesterbrev"
            "AUTORISASJON" -> "Autorisasjon"
            else -> fagdokumentType
        }
    }
}
