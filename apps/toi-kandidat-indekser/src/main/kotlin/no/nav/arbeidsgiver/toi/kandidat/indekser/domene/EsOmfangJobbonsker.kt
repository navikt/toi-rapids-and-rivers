package no.nav.arbeidsgiver.toi.kandidat.indekser.domene

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.JsonNode
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
class EsOmfangJobbonsker(
    private val omfangKode: String,
    private val omfangKodeTekst: String
) {
    override fun equals(other: Any?) = other is EsOmfangJobbonsker && omfangKode == other.omfangKode
            && omfangKodeTekst == other.omfangKodeTekst

    override fun hashCode() = Objects.hash(omfangKode, omfangKodeTekst)

    override fun toString() = ("EsOmfangJobbonsker{" + "omfangKode='" + omfangKode + '\''
            + ", omfangKodeTekst='" + omfangKodeTekst + '\'' + '}')

    companion object {
        fun fraMelding(jobbProfilNode: JsonNode) = jobbProfilNode["omfang"].map(JsonNode::asText)
            .map(Omfang::valueOf)
            .map { omfang ->
                EsOmfangJobbonsker(
                    omfangKode = omfang.name,
                    omfangKodeTekst = omfang.tekst
                )
            }
    }
}

enum class Omfang(val tekst: String) {
    HELTID("Heltid"),
    DELTID("Deltid")
}