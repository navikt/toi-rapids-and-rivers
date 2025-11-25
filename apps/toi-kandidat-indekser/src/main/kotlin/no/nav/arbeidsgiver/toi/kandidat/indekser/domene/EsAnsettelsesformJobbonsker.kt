package no.nav.arbeidsgiver.toi.kandidat.indekser.domene

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
class EsAnsettelsesformJobbonsker(
    @field:JsonProperty private val ansettelsesformKode: String,
    @field:JsonProperty private val ansettelsesformKodeTekst: String
) {
    override fun equals(other: Any?) = other is EsAnsettelsesformJobbonsker && ansettelsesformKode == other.ansettelsesformKode
            && ansettelsesformKodeTekst == other.ansettelsesformKodeTekst

    override fun hashCode() = Objects.hash(ansettelsesformKode, ansettelsesformKodeTekst)

    override fun toString() = ("EsAnsettelsesformJobbonsker{" + "ansettelsesformKode='" + ansettelsesformKode
            + '\'' + ", ansettelsesformKodeTekst='" + ansettelsesformKodeTekst + '\'' + '}')

    companion object {
        fun fraMelding(jobbProfilNode: JsonNode) = jobbProfilNode["ansettelsesformer"].map(JsonNode::asText)
            .map(Ansettelsesform::valueOf)
            .map { ansettelsesformJobbønske ->
                EsAnsettelsesformJobbonsker(
                    ansettelsesformKode = ansettelsesformJobbønske.name,
                    ansettelsesformKodeTekst = ansettelsesformJobbønske.tekst
                )
        }
    }
}

enum class Ansettelsesform(val tekst: String) {
    ENGASJEMENT("Engasjement"),
    FAST("Fast"),
    FERIEJOBB("Feriejobb"),
    PROSJEKT("Prosjekt"),
    SELVSTENDIG_NAERINGSDRIVENDE("Selvstendig næringsdrivende"),
    SESONG("Sesong"),
    VIKARIAT("Vikariat"),
    TRAINEE("Trainee"),
    LAERLING("Lærling"),
    AREMAL("Åremål"),
    FRILANSER("Frilanser"),
    ANNET("Annet");
}