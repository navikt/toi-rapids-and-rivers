package no.nav.arbeidsgiver.toi.kandidat.indekser.domene

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
class EsAnsettelsesformJobbonsker(
    private val ansettelsesformKode: String,
    private val ansettelsesformKodeTekst: String
) {
    override fun equals(other: Any?) = other is EsAnsettelsesformJobbonsker && ansettelsesformKode == other.ansettelsesformKode
            && ansettelsesformKodeTekst == other.ansettelsesformKodeTekst

    override fun hashCode() = Objects.hash(ansettelsesformKode, ansettelsesformKodeTekst)

    override fun toString() = ("EsAnsettelsesformJobbonsker{" + "ansettelsesformKode='" + ansettelsesformKode
            + '\'' + ", ansettelsesformKodeTekst='" + ansettelsesformKodeTekst + '\'' + '}')
}
