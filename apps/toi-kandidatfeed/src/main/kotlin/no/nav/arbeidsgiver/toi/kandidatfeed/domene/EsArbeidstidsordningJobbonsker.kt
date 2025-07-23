package no.nav.arbeidsgiver.toi.kandidatfeed.domene

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
class EsArbeidstidsordningJobbonsker(
    private val arbeidstidsordningKode: String,
    private val arbeidstidsordningKodeTekst: String
) {
    override fun equals(other: Any?) = other is EsArbeidstidsordningJobbonsker && arbeidstidsordningKode == other.arbeidstidsordningKode
            && arbeidstidsordningKodeTekst == other.arbeidstidsordningKodeTekst

    override fun hashCode() = Objects.hash(arbeidstidsordningKode, arbeidstidsordningKodeTekst)

    override fun toString() = ("EsArbeidstidsordningJobbonsker{" + "arbeidstidsordningKode='" + arbeidstidsordningKode
            + '\'' + ", arbeidstidsordningKodeTekst='" + arbeidstidsordningKodeTekst + '\'' + '}')
}
