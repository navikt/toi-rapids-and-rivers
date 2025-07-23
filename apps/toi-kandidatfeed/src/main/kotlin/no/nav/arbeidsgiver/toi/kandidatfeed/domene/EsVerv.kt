package no.nav.arbeidsgiver.toi.kandidatfeed.domene

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
class EsVerv(
    private val fraDato: Date,
    private val tilDato: Date,
    private val organisasjon: String,
    private val tittel: String
) {
    override fun equals(other: Any?) = other is EsVerv && fraDato == other.fraDato && tilDato == other.tilDato
            && organisasjon == other.organisasjon
            && tittel == other.tittel

    override fun hashCode() = Objects.hash(fraDato, tilDato, organisasjon, tittel)

    override fun toString() = ("EsVerv{" + "fraDato=" + fraDato + ", tilDato=" + tilDato + ", organisasjon='"
            + organisasjon + '\'' + ", tittel='" + tittel + '\'' + '}')
}
