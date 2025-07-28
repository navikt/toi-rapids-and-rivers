package no.nav.arbeidsgiver.toi.kandidat.indekser.domene

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
class EsKurs(
    private val tittel: String,
    private val arrangor: String,
    private val omfangEnhet: String?,
    private val omfangVerdi: Int?,
    private val tilDato: Date
) {
    override fun equals(other: Any?) = other is EsKurs && tilDato == other.tilDato
            && tittel == other.tittel && arrangor == other.arrangor
            && omfangEnhet == other.omfangEnhet
            && omfangVerdi == other.omfangVerdi

    override fun hashCode() = Objects.hash(tilDato, tittel, arrangor, omfangEnhet, omfangVerdi)

    override fun toString() = ("EsKurs{" + "tilDato=" + tilDato + ", tittel='" + tittel + '\''
            + ", arrangor='" + arrangor + '\'' + ", omfangEnhet='" + omfangEnhet + '\''
            + ", omfangVerdi=" + omfangVerdi + '\'' + '}')
}
