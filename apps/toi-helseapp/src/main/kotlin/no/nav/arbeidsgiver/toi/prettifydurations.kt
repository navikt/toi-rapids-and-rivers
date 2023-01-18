package no.nav.arbeidsgiver.toi

import java.time.Duration

fun Duration.prettify() = days + hours + minutes + seconds
private val Duration.days: String
    get() = if(toDays()>0) "${toDays()} days " else ""
private val Duration.hours: String
    get() = if(days.isNotEmpty()||toHoursPart()>0) "${toHoursPart()} hours " else ""
private val Duration.minutes: String
    get() = if(hours.isNotEmpty()||toMinutesPart()>0) "${toMinutesPart()} minutes " else ""
private val Duration.seconds: String
    get() = if(minutes.isNotEmpty()||toSecondsPart()>0) "${toSecondsPart()} seconds" else "0"