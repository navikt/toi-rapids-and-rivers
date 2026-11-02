package no.nav.arbeidsgiver.toi.http

import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.http.HttpResponse
import java.time.Duration

private val log = LoggerFactory.getLogger("no.nav.arbeidsgiver.toi.http.Retry")

/**
 * Prøver [kall] på nytt ved [IOException] (inkludert timeout) og 5xx-svar, med doblende ventetid mellom forsøkene.
 * Siste forsøk kjøres uten å fange noe, slik at kallende kode får siste svar eller unntak som før.
 */
fun <T> medRetry(
    maksForsøk: Int = 3,
    ventetid: Duration = Duration.ofMillis(1000),
    kall: () -> HttpResponse<T>,
): HttpResponse<T> {
    repeat(maksForsøk - 1) { i ->
        try {
            val response = kall()
            if (response.statusCode() !in 500..599) return response
            log.warn("Statuskode ${response.statusCode()} fra ${response.uri()}, forsøk ${i + 1} av $maksForsøk")
        } catch (e: IOException) {
            log.warn("${e.javaClass.simpleName} ved HTTP-kall, forsøk ${i + 1} av $maksForsøk", e)
        }
        Thread.sleep(ventetid.multipliedBy(1L shl i).toMillis())
    }
    return kall()
}
