package no.nav.arbeidsgiver.toi

import java.time.ZonedDateTime

data class Kandidat(
    val cv: Any?,
    val oppfølgingsinformasjon: Oppfølgingsinformasjon?,
    val oppfølgingsperiode: Oppfølgingsperiode?,
)

data class Oppfølgingsperiode(
    val startDato: ZonedDateTime,
    val sluttDato: ZonedDateTime?
)

data class Oppfølgingsinformasjon(
    val erDoed: Boolean,
    val sperretAnsatt: Boolean,
    val formidlingsgruppe: String,
)
