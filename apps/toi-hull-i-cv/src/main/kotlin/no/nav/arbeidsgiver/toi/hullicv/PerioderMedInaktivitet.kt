package no.nav.arbeidsgiver.toi.hullicv

import java.time.LocalDate

data class PerioderMedInaktivitet(
    val fĂ¸rsteDagIInnevĂ¦rendeInaktivePeriode: LocalDate?,
    val sluttdatoerForInaktivePerioder: List<LocalDate>
)
