package no.nav.arbeidsgiver.toi.organisasjonsenhet

import java.time.LocalDate

data class PerioderMedInaktivitet(
    val førsteDagIInneværendeInaktivePeriode: LocalDate?,
    val sluttdatoerForInaktivePerioder: List<LocalDate>
)
