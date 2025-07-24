package no.nav.arbeidsgiver.toi.kandidat.indekser.domene

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
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
}
