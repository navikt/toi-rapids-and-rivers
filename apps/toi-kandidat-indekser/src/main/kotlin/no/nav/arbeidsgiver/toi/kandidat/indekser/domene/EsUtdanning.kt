package no.nav.arbeidsgiver.toi.kandidat.indekser.domene

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
class EsUtdanning(
    private val fraDato: Date,
    private val tilDato: Date,
    private val utdannelsessted: String,
    private val nusKode: String,
    @JsonInclude(JsonInclude.Include.NON_EMPTY) private val nusKodeGrad: String,
    private val alternativGrad: String,
    private val yrkestatus: String?,
    private val beskrivelse: String?
) {
    constructor(
        fraDato: Date, tilDato: Date, utdannelsessted: String, nusKode: String,
        nusKodeGrad: String, alternativGrad: String
    ): this(
        fraDato,
        tilDato,
        utdannelsessted,
        nusKode,
        nusKodeGrad,
        alternativGrad,
        null,
        null
    )

    override fun equals(other: Any?) = other is EsUtdanning && fraDato == other.fraDato && tilDato == other.tilDato
            && utdannelsessted == other.utdannelsessted
            && nusKode == other.nusKode && nusKodeGrad == other.nusKodeGrad
            && alternativGrad == other.alternativGrad
            && beskrivelse == other.beskrivelse

    override fun hashCode() =
        Objects.hash(fraDato, tilDato, utdannelsessted, nusKode, nusKodeGrad, alternativGrad, beskrivelse)

    override fun toString() = ("EsUtdanning{" + "fraDato=" + fraDato + ", tilDato=" + tilDato + ", utdannelsessted='"
            + utdannelsessted + '\'' + ", nusKode='" + nusKode + '\'' + ", nusKodeGrad='" + nusKodeGrad
            + '\'' + ", alternativGrad='" + alternativGrad + '\'' + ", beskrivelse='" + beskrivelse + '\'' + '}')
}
