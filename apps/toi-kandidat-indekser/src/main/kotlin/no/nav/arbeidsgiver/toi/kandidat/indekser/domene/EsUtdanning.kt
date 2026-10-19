package no.nav.arbeidsgiver.toi.kandidat.indekser.domene

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import tools.jackson.databind.JsonNode
import java.time.OffsetDateTime
import java.time.YearMonth
import java.util.Objects

@JsonIgnoreProperties(ignoreUnknown = true)
class EsUtdanning(
    @field:JsonProperty private val fraDato: YearMonth,
    @field:JsonProperty private val tilDato: YearMonth?,
    @field:JsonProperty private val utdannelsessted: String,
    @field:JsonProperty private val nusKode: String?,
    @field:JsonProperty private val alternativGrad: String,
    @field:JsonProperty private val yrkestatus: UtdannelseYrkestatus?,
    @field:JsonProperty private val beskrivelse: String?
) {
    constructor(
        fraDato: YearMonth, tilDato: YearMonth?, utdannelsessted: String, nusKode: String?,
        alternativGrad: String
    ): this(
        fraDato,
        tilDato,
        utdannelsessted,
        nusKode,
        alternativGrad,
        null,
        null
    )

    override fun equals(other: Any?) = other is EsUtdanning && fraDato == other.fraDato && tilDato == other.tilDato
            && utdannelsessted == other.utdannelsessted
            && nusKode == other.nusKode
            && alternativGrad == other.alternativGrad
            && beskrivelse == other.beskrivelse

    override fun hashCode() =
        Objects.hash(fraDato, tilDato, utdannelsessted, nusKode, alternativGrad, beskrivelse)

    override fun toString() = ("EsUtdanning{" + "fraDato=" + fraDato + ", tilDato=" + tilDato + ", utdannelsessted='"
            + utdannelsessted + '\'' + ", nusKode='" + nusKode + '\'' + ", alternativGrad='" + alternativGrad + '\''
            + ", beskrivelse='" + beskrivelse + '\'' + '}')

    companion object {
        fun fraMelding(cvNode: JsonNode) = cvNode["utdannelse"].values().map { utdannelseNode ->
            EsUtdanning(
                fraDato = YearMonth.parse(utdannelseNode["fraTidspunkt"].asString()),
                tilDato = utdannelseNode["tilTidspunkt"].asString(null)?.let(YearMonth::parse),
                utdannelsessted = utdannelseNode["laerested"].asString(),
                nusKode = utdannelseNode["nuskodeGrad"].asString(),
                alternativGrad = utdannelseNode["utdanningsretning"].asString(),
                beskrivelse = utdannelseNode["beskrivelse"].asString(""),
                yrkestatus = utdannelseNode["utdannelseYrkestatus"].asString(null)?.let(UtdannelseYrkestatus::valueOf) ?: UtdannelseYrkestatus.INGEN
            )
        }
    }
}

enum class UtdannelseYrkestatus {
    SVENNEBREV_FAGBREV, MESTERBREV, INGEN;
}