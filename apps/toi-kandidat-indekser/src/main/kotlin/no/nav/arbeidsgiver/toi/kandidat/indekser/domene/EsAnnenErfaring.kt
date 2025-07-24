package no.nav.arbeidsgiver.toi.kandidat.indekser.domene

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
class EsAnnenErfaring(
    private val fraDato: Date,
    private val tilDato: Date,
    private val beskrivelse: String,
    private val rolle: String? = null
) {
    override fun equals(other: Any?) = other is EsAnnenErfaring && fraDato == other.fraDato && tilDato == other.tilDato
            && beskrivelse == other.beskrivelse && rolle == other.rolle

    override fun hashCode() = Objects.hash(fraDato, tilDato, beskrivelse, rolle)

    override fun toString() = ("EsAnnenErfaring{" + "fraDato=" + fraDato + ", tilDato=" + tilDato + ", beskrivelse='"
            + beskrivelse + '\'' + ", rolle='" + rolle + '\'' + '}')
}
