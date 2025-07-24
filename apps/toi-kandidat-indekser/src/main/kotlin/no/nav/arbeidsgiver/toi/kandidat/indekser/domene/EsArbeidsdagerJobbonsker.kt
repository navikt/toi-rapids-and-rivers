package no.nav.arbeidsgiver.toi.kandidat.indekser.domene

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
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
}
