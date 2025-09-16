package no.nav.arbeidsgiver.toi.kandidat.indekser.domene

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.Objects

@JsonIgnoreProperties(ignoreUnknown = true)
class EsKurs(
    private val tittel: String,
    private val arrangor: String,
    private val omfangEnhet: String?,
    private val omfangVerdi: Int?,
    private val tilDato: LocalDate
) {
    override fun equals(other: Any?) = other is EsKurs && tilDato == other.tilDato
            && tittel == other.tittel && arrangor == other.arrangor
            && omfangEnhet == other.omfangEnhet
            && omfangVerdi == other.omfangVerdi

    override fun hashCode() = Objects.hash(tilDato, tittel, arrangor, omfangEnhet, omfangVerdi)

    override fun toString() = ("EsKurs{" + "tilDato=" + tilDato + ", tittel='" + tittel + '\''
            + ", arrangor='" + arrangor + '\'' + ", omfangEnhet='" + omfangEnhet + '\''
            + ", omfangVerdi=" + omfangVerdi + '\'' + '}')

    companion object {
        fun fraMelding(cvNode: JsonNode) = cvNode["kurs"].map { kursNode ->
            EsKurs(
                tittel = kursNode["tittel"].asText(),
                arrangor = kursNode["utsteder"].asText(),
                omfangEnhet = kursNode["varighetEnhet"]?.asText("") ?: "",
                omfangVerdi = kursNode["varighet"]?.asInt(),
                tilDato = kursNode["tidspunkt"].yyyyMMddTilLocalDate()
            )
        }
    }
}
