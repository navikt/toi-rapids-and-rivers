package no.nav.arbeidsgiver.toi

import java.time.LocalDate

data class PerioderMedInaktivitet(
    val førsteDagIInneværendeInaktivePeriode: LocalDate?,
    val sluttdatoerForInaktivePerioder: List<LocalDate>
)
