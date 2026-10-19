package no.nav.arbeidsgiver.toi.kandidat.indekser.domene

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import tools.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
class EsGeografiJobbonsker(
    @field:JsonProperty private val geografiKodeTekst: String,
    @field:JsonProperty private val geografiKode: String
) {
    override fun equals(other: Any?) = other is EsGeografiJobbonsker && geografiKodeTekst == other.geografiKodeTekst
            && geografiKode == other.geografiKode

    override fun hashCode() = Objects.hash(geografiKodeTekst, geografiKode)

    companion object {
        fun fraMelding(jobbProfilNode: JsonNode, packet: JsonMessage) = jobbProfilNode.path("geografi").values().map { geografi ->
            val geografiKode = geografi["kode"].asString()
            EsGeografiJobbonsker(
                geografiKodeTekst = packet["geografi.geografi"][geografiKode]?.asString(null) ?: geografi["sted"].asString(),
                geografiKode = geografiKode
            )
        }
    }
}
