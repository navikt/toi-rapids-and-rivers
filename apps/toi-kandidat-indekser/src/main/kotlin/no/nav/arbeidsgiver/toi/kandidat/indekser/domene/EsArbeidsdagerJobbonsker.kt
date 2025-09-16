package no.nav.arbeidsgiver.toi.kandidat.indekser.domene

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.JsonNode
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
class EsArbeidsdagerJobbonsker(
    private val arbeidsdagerKode: String,
    private val arbeidsdagerKodeTekst: String
) {
    override fun equals(other: Any?) = other is EsArbeidsdagerJobbonsker && arbeidsdagerKode == other.arbeidsdagerKode
            && arbeidsdagerKodeTekst == other.arbeidsdagerKodeTekst

    override fun hashCode() = Objects.hash(arbeidsdagerKode, arbeidsdagerKodeTekst)

    override fun toString() = ("EsArbeidstidsordningJobbonsker{" + "arbeidsdagerKode='" + arbeidsdagerKode
            + '\'' + ", arbeidsdagerKodeTekst='" + arbeidsdagerKodeTekst + '\'' + '}')

    companion object {
        fun fraMelding(jobbProfilNode: JsonNode) = jobbProfilNode["arbeidsdager"].map(JsonNode::asText)
            .map(Arbeidsdager::valueOf)
            .map { arbeidstidsordning ->
                EsArbeidsdagerJobbonsker(
                    arbeidsdagerKode = arbeidstidsordning.name,
                    arbeidsdagerKodeTekst = arbeidstidsordning.tekst
                )
            }
    }
}

enum class Arbeidsdager(val tekst: String) {
    LOERDAG("Lørdag"),
    SOENDAG("Søndag"),
    UKEDAGER("Ukedager")
}