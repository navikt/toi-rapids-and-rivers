package no.nav.arbeidsgiver.toi.logging

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory

@Suppress("unused")
val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)

/**
 *  Brukes i Kotlin-kode som ikke er inne i en klasse, typisk i en "top level function".

 * Kalles fra den filen du ønsker å logge i slik:
 *```
 * import no.nav.arbeidsgiver.toi.noClassLogger
 * private val log = noClassLogger()
 * fun myToplevelFunction() {
 *      log.info("yada yada yada")
 *      ...
 * }
 *```
 * @return`En Logger med samme navn som Kotlin-filen du logger fra, prefikset med pakkenavn.
 * */
@Suppress("unused")
fun noClassLogger(): Logger {
    val callerClassName = Throwable().stackTrace[1].className
    return LoggerFactory.getLogger(callerClassName)
}


@Suppress("unused")
class TeamLogLogger private constructor(private val l: Logger) {
    fun info(msg: String) {
        l.info(m, msg)
    }

    fun info(msg: String, t: Throwable) {
        l.info(m, msg, t)
    }

    fun warn(msg: String) {
        l.warn(m, msg)
    }

    fun warn(msg: String, t: Throwable) {
        l.warn(m, msg, t)
    }

    fun error(msg: String) {
        l.error(m, msg)
    }

    fun error(msg: String, t: Throwable) {
        l.error(m, msg, t)
    }

    companion object {
        private val m = MarkerFactory.getMarker("TEAM_LOGS")

        fun teamlog(l: Logger): TeamLogLogger = TeamLogLogger(l)
    }
}
