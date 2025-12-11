package no.nav.arbeidsgiver.toi.kandidat.indekser.domene

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
class EsArbeidstidsordningJobbonsker(
    @field:JsonProperty private val arbeidstidsordningKode: String,
    @field:JsonProperty private val arbeidstidsordningKodeTekst: String
) {
    override fun equals(other: Any?) = other is EsArbeidstidsordningJobbonsker && arbeidstidsordningKode == other.arbeidstidsordningKode
            && arbeidstidsordningKodeTekst == other.arbeidstidsordningKodeTekst

    override fun hashCode() = Objects.hash(arbeidstidsordningKode, arbeidstidsordningKodeTekst)

    override fun toString() = ("EsArbeidstidsordningJobbonsker{" + "arbeidstidsordningKode='" + arbeidstidsordningKode
            + '\'' + ", arbeidstidsordningKodeTekst='" + arbeidstidsordningKodeTekst + '\'' + '}')

    companion object {
        fun fraMelding(jobbProfilNode: JsonNode) = jobbProfilNode["arbeidstidsordninger"].map(JsonNode::asText)
            .map(Arbeidstidsordning::valueOf)
            .map { arbeidstidsordning ->
                EsArbeidstidsordningJobbonsker(
                    arbeidstidsordningKode = arbeidstidsordning.name,
                    arbeidstidsordningKodeTekst = arbeidstidsordning.tekst
                )
            }
    }
}

enum class Arbeidstidsordning(val tekst: String) {
    SKIFT("Skift"),
    TURNUS("Turnus"),
    VAKT("Vakt")
}