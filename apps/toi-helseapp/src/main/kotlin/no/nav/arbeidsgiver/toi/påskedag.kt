package no.nav.arbeidsgiver.toi

import java.time.LocalDate
import java.time.Month

fun påskedag(year: Int): LocalDate {
    val a = year % 19
    val b = year % 4
    val c = year % 7
    val k = year / 100
    val p = (13 + 8 * k) / 25
    val q = k / 4
    val M = (15 - p + k - q) % 30
    val N = (4 + k - q) % 7
    val d = (19 * a + M) % 30
    val e = (2 * b + 4 * c + 6 * d + N) % 7
    return LocalDate.of(year,Month.MARCH,22).plusDays((d+e).toLong())
}
fun LocalDate.erPåske(): Boolean = påskedag(year).let { it.minusDays(4).datesUntil(it.plusDays(2)) }.anyMatch { it == this }
fun LocalDate.erPinse(): Boolean = påskedag(year).plusDays(50) == this
fun LocalDate.erKristiHimmelfartsdag(): Boolean = påskedag(year).plusDays(39) == this