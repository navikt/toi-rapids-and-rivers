package no.nav.arbeidsgiver.toi.kandidat.indekser.domene

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import java.time.OffsetDateTime
import java.util.Objects

@JsonIgnoreProperties(ignoreUnknown = true)
class EsKompetanse(
    @field:JsonProperty private val fraDato: OffsetDateTime?,
    @field:JsonProperty private val kompKode: String?,
    @field:JsonProperty @JsonInclude(JsonInclude.Include.NON_EMPTY) private val kompKodeNavn: String,
    @field:JsonProperty private val alternativtNavn: String?,
    @field:JsonProperty private val beskrivelse: String?,
    sokeNavn: List<String>
): EnAvFlereSamledeKompetaser {
    @field:JsonProperty private val sokeNavn = sokeNavn + kompKodeNavn

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

    companion object {
        fun fraMelding(jobbProfilNode: JsonNode, packet: JsonMessage) =
            jobbProfilNode["kompetanser"].map(JsonNode::asText).map { kompetanse ->
                EsKompetanse(
                    fraDato = null,
                    kompKode = null,
                    kompKodeNavn = kompetanse,
                    alternativtNavn = kompetanse,
                    beskrivelse = "",
                    sokeNavn = packet["ontologi.kompetansenavn"][kompetanse].let { synonymer ->
                        synonymer["synonymer"].map(JsonNode::asText) +
                                synonymer["merGenerell"].map(JsonNode::asText)
                    }
                )
            }
    }
}
