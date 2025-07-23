package no.nav.arbeidsgiver.toi.kandidatfeed.domene

import java.util.*

class EsPerioderMedInaktivitet(
    private val startdatoForInnevarendeInaktivePeriode: Date,
    private val sluttdatoerForInaktivePerioderPaToArEllerMer: List<Date>
) {
    override fun equals(other: Any?) = other is EsPerioderMedInaktivitet && this.startdatoForInnevarendeInaktivePeriode == other.startdatoForInnevarendeInaktivePeriode && this.sluttdatoerForInaktivePerioderPaToArEllerMer == other.sluttdatoerForInaktivePerioderPaToArEllerMer

    override fun hashCode() = Objects.hash(
        this.startdatoForInnevarendeInaktivePeriode,
        this.sluttdatoerForInaktivePerioderPaToArEllerMer
    )
}
