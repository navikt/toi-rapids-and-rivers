package no.nav.toi.stilling.publiser.arbeidsplassen

import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

fun ZonedDateTime.toIsoDateTimeString(): String = this.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
fun LocalDateTime.toIsoDateTimeString(): String = this.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
