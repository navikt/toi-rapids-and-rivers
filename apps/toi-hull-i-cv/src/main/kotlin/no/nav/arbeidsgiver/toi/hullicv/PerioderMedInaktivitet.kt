package no.nav.arbeidsgiver.toi.hullicv

import java.time.LocalDate

data class PerioderMedInaktivitet(
    val førsteDagIInneværendeInaktivePeriode: LocalDate?,
    val sluttdatoerForInaktivePerioder: List<LocalDate>
)
