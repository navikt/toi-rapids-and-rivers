package no.nav.arbeidsgiver.toi.kandidat.indekser.domene

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import no.nav.pam.geography.ArenaGeography
import no.nav.pam.geography.ArenaGeographyDAO
import no.nav.pam.geography.CountryDAO
import no.nav.pam.geography.CountyDAO
import no.nav.pam.geography.PostDataDAO
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
        private val arenaGeographyDAO = ArenaGeographyDAO(CountryDAO(), PostDataDAO())
        fun fraMelding(jobbProfilNode: JsonNode) = jobbProfilNode["geografi"].map { geografi ->
            val geografiKode = geografi["kode"].asText()
            EsGeografiJobbonsker(
                geografiKodeTekst = arenaGeographyDAO.findArenaGeography(geografiKode).map(ArenaGeography::getCapitalizedName).orElse(geografi["sted"].asText()),
                geografiKode = geografiKode
            )
        }
    }
}
