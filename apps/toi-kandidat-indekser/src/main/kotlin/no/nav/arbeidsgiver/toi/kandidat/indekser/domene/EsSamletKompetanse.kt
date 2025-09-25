package no.nav.arbeidsgiver.toi.kandidat.indekser.domene

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

interface EnAvFlereSamledeKompetaser{
    fun tilSamletKompetanse(): List<EsSamletKompetanse>
}

@JsonIgnoreProperties(ignoreUnknown = true)
class EsSamletKompetanse(
    @field:JsonProperty @JsonInclude(JsonInclude.Include.NON_EMPTY) private val samletKompetanseTekst: String
) {
    override fun equals(other: Any?) = other is EsSamletKompetanse && samletKompetanseTekst == other.samletKompetanseTekst

    override fun hashCode() = Objects.hash(samletKompetanseTekst)

    override fun toString() = "EsSamletKompetanse{" + "samletKompetanseTekst='" + samletKompetanseTekst + '\'' + '}'
}
