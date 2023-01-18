package no.nav.arbeidsgiver.toi

import java.time.Duration

fun Duration.prettify() = dager + timer + minutter + sekunder
private val Duration.dager: String
    get() = if(toDays()>0) "${toDays()} dager " else ""
private val Duration.timer: String
    get() = if(dager.isNotEmpty()||toHoursPart()>0) "${toHoursPart()} timer " else ""
private val Duration.minutter: String
    get() = if(timer.isNotEmpty()||toMinutesPart()>0) "${toMinutesPart()} minutter " else ""
private val Duration.sekunder: String
    get() = if(minutter.isNotEmpty()||toSecondsPart()>0) "${toSecondsPart()} sekunder" else "0"