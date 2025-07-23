package no.nav.arbeidsgiver.toi.kandidatfeed.domene

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import java.util.*

interface EnAvFlereSamledeKompetaser{
    fun tilSamletKompetanse(): List<EsSamletKompetanse>
}

@JsonIgnoreProperties(ignoreUnknown = true)
class EsSamletKompetanse(
    @JsonInclude(JsonInclude.Include.NON_EMPTY) private val samletKompetanseTekst: String
) {
    override fun equals(other: Any?) = other is EsSamletKompetanse && samletKompetanseTekst == other.samletKompetanseTekst

    override fun hashCode() = Objects.hash(samletKompetanseTekst)

    override fun toString() = "EsSamletKompetanse{" + "samletKompetanseTekst='" + samletKompetanseTekst + '\'' + '}'
}
