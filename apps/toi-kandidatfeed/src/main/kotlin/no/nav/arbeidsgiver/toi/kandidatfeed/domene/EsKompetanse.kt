package no.nav.arbeidsgiver.toi.kandidatfeed.domene

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
class EsKompetanse(
    private val fraDato: Date?,
    private val kompKode: String?,
    @JsonInclude(JsonInclude.Include.NON_EMPTY) private val kompKodeNavn: String,
    private val alternativtNavn: String,
    private val beskrivelse: String,
    sokeNavn: List<String>
): EnAvFlereSamledeKompetaser {
    private val sokeNavn = sokeNavn + kompKodeNavn

    constructor(kompetanse: String, sokeNavn: List<String>) : this(
        null,
        null,
        kompetanse,
        kompetanse,
        "",
        sokeNavn
    )

    override fun equals(other: Any?) = other is EsKompetanse && fraDato == other.fraDato && kompKode == other.kompKode
            && kompKodeNavn == other.kompKodeNavn
            && sokeNavn == other.sokeNavn
            && alternativtNavn == other.alternativtNavn
            && beskrivelse == other.beskrivelse

    override fun hashCode() = Objects.hash(fraDato, kompKode, kompKodeNavn, alternativtNavn, beskrivelse)

    override fun toString() = ("EsKompetanse{" + "fraDato=" + fraDato + ", kompKode='" + kompKode + '\''
            + ", kompKodeNavn='" + kompKodeNavn + '\'' + ", alternativtNavn='" + alternativtNavn
            + '\'' + ", beskrivelse='" + beskrivelse + '\'' + '}')

    override fun tilSamletKompetanse() = sokeNavn.map(::EsSamletKompetanse)
}
