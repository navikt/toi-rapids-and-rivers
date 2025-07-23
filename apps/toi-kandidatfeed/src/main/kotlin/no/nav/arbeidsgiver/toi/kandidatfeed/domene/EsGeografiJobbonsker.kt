package no.nav.arbeidsgiver.toi.kandidatfeed.domene

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
class EsGeografiJobbonsker(
    private val geografiKodeTekst: String,
    private val geografiKode: String
) {
    override fun equals(other: Any?) = other is EsGeografiJobbonsker && geografiKodeTekst == other.geografiKodeTekst
            && geografiKode == other.geografiKode

    override fun hashCode() = Objects.hash(geografiKodeTekst, geografiKode)
}
