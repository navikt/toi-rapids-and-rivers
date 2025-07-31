package no.nav.arbeidsgiver.toi.kandidat.indekser.domene

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.OffsetDateTime
import java.util.Objects

@JsonIgnoreProperties(ignoreUnknown = true)
class EsVerv(
    private val fraDato: OffsetDateTime,
    private val tilDato: OffsetDateTime,
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
