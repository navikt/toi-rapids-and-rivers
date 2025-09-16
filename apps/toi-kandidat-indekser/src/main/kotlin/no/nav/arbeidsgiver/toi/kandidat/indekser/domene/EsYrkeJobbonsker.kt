package no.nav.arbeidsgiver.toi.kandidat.indekser.domene

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
class EsYrkeJobbonsker(
    private val styrkKode: String?,
    @JsonInclude(JsonInclude.Include.NON_EMPTY) private val styrkBeskrivelse: String,
    private val primaertJobbonske: Boolean,
    sokeTitler: List<String>
) {
    private val sokeTitler = sokeTitler + styrkBeskrivelse

    override fun equals(other: Any?) = other is EsYrkeJobbonsker && this.primaertJobbonske == other.primaertJobbonske && styrkKode == other.styrkKode
            && styrkBeskrivelse == other.styrkBeskrivelse

    override fun hashCode() = Objects.hash(styrkKode, styrkBeskrivelse, this.primaertJobbonske)

    override fun toString() = ("EsYrkeJobbonsker{" + "styrkKode='" + styrkKode + '\'' + ", styrkBeskrivelse='"
            + styrkBeskrivelse + '\'' + ", primaertJobbonske=" + this.primaertJobbonske + '}')

    companion object {
        fun fraMelding(jobbProfilNode: JsonNode, packet: JsonMessage) =
            jobbProfilNode["stillinger"].map (JsonNode::asText).map { stilling ->
                EsYrkeJobbonsker(
                    styrkKode = null,
                    styrkBeskrivelse = stilling,
                    primaertJobbonske = false,
                    sokeTitler = packet["ontologi.stillingstittel"][stilling].let { synonymer ->
                        synonymer["synonymer"].map(JsonNode::asText) +
                                synonymer["merGenerell"].map(JsonNode::asText)
                    }
                )
            }
    }
}
