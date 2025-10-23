package no.nav.arbeidsgiver.toi.kandidat.indekser.domene

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import no.nav.arbeidsgiver.toi.kandidat.indekser.geografi.GeografiKlient
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
        fun fraMelding(jobbProfilNode: JsonNode, geografiKlient: GeografiKlient) = jobbProfilNode["geografi"].map { geografi ->
            val geografiKode = geografi["kode"].asText()
            EsGeografiJobbonsker(
                geografiKodeTekst = geografiKlient.findArenaGeography(geografiKode)?.kapitalisertNavn ?: geografi["sted"].asText(),
                geografiKode = geografiKode
            )
        }
    }
}
