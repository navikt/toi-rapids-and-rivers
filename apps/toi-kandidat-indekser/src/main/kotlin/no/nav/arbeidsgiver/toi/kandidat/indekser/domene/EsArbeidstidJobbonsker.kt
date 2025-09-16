package no.nav.arbeidsgiver.toi.kandidat.indekser.domene

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.JsonNode
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
class EsArbeidstidJobbonsker(
    private val arbeidstidKode: String,
    private val arbeidstidKodeTekst: String
) {
    override fun equals(other: Any?) = other is EsArbeidstidJobbonsker && arbeidstidKode == other.arbeidstidKode
            && arbeidstidKodeTekst == other.arbeidstidKodeTekst

    override fun hashCode() = Objects.hash(arbeidstidKode, arbeidstidKodeTekst)

    override fun toString() = ("EsArbeidstidsordningJobbonsker{" + "arbeidstidKode='" + arbeidstidKode
            + '\'' + ", arbeidstidKodeTekst='" + arbeidstidKodeTekst + '\'' + '}')

    companion object {
        fun fraMelding(jobbProfilNode: JsonNode) = jobbProfilNode["arbeidstider"].map(JsonNode::asText)
            .map(Arbeidstider::valueOf)
            .map { arbeidstidsordning ->
                EsArbeidstidJobbonsker(
                    arbeidstidKode = arbeidstidsordning.name,
                    arbeidstidKodeTekst = arbeidstidsordning.tekst
                )
            }
    }
}

enum class Arbeidstider(val tekst: String) {
    DAGTID("Dagtid"),
    KVELD("Kveld"),
    NATT("Natt")
}