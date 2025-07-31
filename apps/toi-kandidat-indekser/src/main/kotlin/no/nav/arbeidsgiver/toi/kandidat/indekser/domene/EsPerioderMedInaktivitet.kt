package no.nav.arbeidsgiver.toi.kandidat.indekser.domene

import java.time.OffsetDateTime
import java.util.Objects

class EsPerioderMedInaktivitet(
    private val startdatoForInnevarendeInaktivePeriode: OffsetDateTime,
    private val sluttdatoerForInaktivePerioderPaToArEllerMer: List<OffsetDateTime>
) {
    override fun equals(other: Any?) = other is EsPerioderMedInaktivitet && this.startdatoForInnevarendeInaktivePeriode == other.startdatoForInnevarendeInaktivePeriode && this.sluttdatoerForInaktivePerioderPaToArEllerMer == other.sluttdatoerForInaktivePerioderPaToArEllerMer

    override fun hashCode() = Objects.hash(
        this.startdatoForInnevarendeInaktivePeriode,
        this.sluttdatoerForInaktivePerioderPaToArEllerMer
    )
}
