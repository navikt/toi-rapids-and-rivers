package no.nav.arbeidsgiver.toi.hullicv

import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.FromStringDeserializer
import java.time.*


class Cv(
    private val utdannelse: List<CVPeriode>,
    private val arbeidserfaring: List<CVPeriode>,
    private val foedselsdato: LocalDate
) {
    fun tilPerioderMedInaktivitet(): PerioderMedInaktivitet {
        val aktivePerioder = perioderMedAktivitet()

        val aktivePerioderUtflatet = flattened(aktivePerioder)

        val (førsteDagIInneværendeInaktivePeriode, inaktivePerioder) = perioderMedInaktivitet(
            aktivePerioderUtflatet
        )

        val inaktivePerioderMedVarighetToÅrEllerMer = inaktivePerioder.filter { it.varighet().years >= 2 }

        val inaktivePerioderEtterEnVissAlder =
            inaktivePerioderEtterEnVissAlder(inaktivePerioderMedVarighetToÅrEllerMer, foedselsdato)

        return perioderMedInaktivitet(førsteDagIInneværendeInaktivePeriode, inaktivePerioderEtterEnVissAlder)
    }

    private fun perioderMedAktivitet(): List<AktivPeriode> {
        val arbeidserfaringer = this.arbeidserfaring.map(CVPeriode::tilAktivPeriode)
        val utdannelser = utdannelse.map(CVPeriode::tilAktivPeriode)
        return arbeidserfaringer + utdannelser
    }
}

class CVPeriode(
    @JsonDeserialize(using = YearMonthDeserializerWithDayOne::class)
    val fraTidspunkt: LocalDate?,
    @JsonDeserialize(using = YearMonthDeserializerWithDayOne::class)
    val tilTidspunkt: LocalDate?
) {
    fun tilAktivPeriode() = AktivPeriode(
        fraTidspunkt ?: LocalDate.MIN,
        tilTidspunkt ?: LocalDate.MAX
    )

    fun tilArbeidsmarkedJson() = """{
            "fraTidspunkt": ${fraTidspunkt?.let { """"${YearMonth.from(it)}"""" } ?: "null"},
            "tilTidspunkt": ${tilTidspunkt?.let { """"${YearMonth.from(it)}"""" } ?: "null"}
          }
    """.trimIndent()
}

class YearMonthDeserializerWithDayOne : FromStringDeserializer<LocalDate?>(LocalDate::class.java) {
    override fun _deserialize(value: String?, ctxt: DeserializationContext?): LocalDate? {
        if (value == null) {
            return null
        }
        return LocalDate.parse("$value-01") // Bruk alltid 1. i måneden
    }
}

private fun inaktivePerioderEtterEnVissAlder(
    perioder: List<InaktivPeriode>,
    foedselsdato: LocalDate
): List<InaktivPeriode> {
    val startdatoForVideregåendePlussEttÅr = LocalDate.of(foedselsdato.year + 17, 8, 31)

    return perioder.filter { p ->
        p.sisteDag.isAfterOrEqual(startdatoForVideregåendePlussEttÅr)
    }
}

data class AktivPeriode(val førsteDag: LocalDate, val sisteDag: LocalDate) {
    fun inneholder(dato: LocalDate): Boolean =
        dato.isAfterOrEqual(førsteDag) && dato.isBeforeOrEqual(sisteDag)

    fun overlapperMed(other: AktivPeriode): Boolean =
        inneholder(other.sisteDag) || inneholder(other.førsteDag) || other.inneholder(sisteDag) || other.inneholder(
            førsteDag
        )
}


private data class InaktivPeriode(val førsteDag: LocalDate, val sisteDag: LocalDate) {
    fun varighet(): Period = Period.between(førsteDag, sisteDag.plusOneDayOrMax())
}


/**
 * @return Perioder med aktivitet som ikke overlapper, alltid har inaktivitet mellom seg og er sortert på startdato tidligste først
 */
private fun flattened(potensieltOverlappendePerioder: Collection<AktivPeriode>): List<AktivPeriode> {
    val aktivePerioder: MutableSet<AktivPeriode> = mutableSetOf()

    fun add(ny: AktivPeriode) {
        val overlappendeGammel =
            aktivePerioder.filter { it.overlapperMed(ny) }
        if (overlappendeGammel.isEmpty()) {
            aktivePerioder.add(ny)
        } else {
            aktivePerioder.removeAll(overlappendeGammel)
            aktivePerioder.add(
                AktivPeriode(
                    (overlappendeGammel.map(AktivPeriode::førsteDag) + ny.førsteDag).minOrNull() ?: throw Exception(),
                    (overlappendeGammel.map(AktivPeriode::sisteDag) + ny.sisteDag).maxOrNull() ?: throw Exception()
                )
            )

        }
    }

    potensieltOverlappendePerioder.forEach(::add)
    return aktivePerioder.toList().sortedBy { it.førsteDag }
}


/**
 * @param aktivePerioder Forutsetter at periodene i denne listen ikke overlapper, alltid har inaktivitet mellom seg og er sortert på startdato tidligste først
 * @return Et tuppel av 1) startdato for inneværende inaktive periode, og 2) alle andre inaktive perioder
 */
private fun perioderMedInaktivitet(aktivePerioder: List<AktivPeriode>): Pair<LocalDate, List<InaktivPeriode>> {

    tailrec fun perioderMedInaktivitet(
        accInaktivePerioder: List<InaktivPeriode>,
        aktivePerioder: List<AktivPeriode>
    ): Pair<LocalDate, List<InaktivPeriode>> {
        return when {
            aktivePerioder.isEmpty() -> {
                Pair(LocalDate.MIN, listOf())
            }

            accInaktivePerioder.isEmpty() -> {
                val førsteInaktivePeriode = InaktivPeriode(
                    førsteDag = LocalDate.MIN,
                    sisteDag = aktivePerioder.firstOrNull()?.førsteDag?.minusOneDayOrMin() ?: LocalDate.MAX
                )
                perioderMedInaktivitet(listOf(førsteInaktivePeriode), aktivePerioder)
            }

            aktivePerioder.size == 1 -> {
                Pair(aktivePerioder.first().sisteDag.plusOneDayOrMax(), accInaktivePerioder)
            }

            else -> {
                val (head, tail) = aktivePerioder.headTail()
                val neck = tail.first()
                val nyInaktivPeriode =
                    InaktivPeriode(head.sisteDag.plusOneDayOrMax(), neck.førsteDag.minusOneDayOrMin())
                perioderMedInaktivitet(accInaktivePerioder + nyInaktivPeriode, tail)
            }
        }
    }

    return perioderMedInaktivitet(listOf(), aktivePerioder)
}


private fun perioderMedInaktivitet(
    førsteDagIInneværendeInaktivePeriode: LocalDate,
    inaktivePerioder: List<InaktivPeriode>
): PerioderMedInaktivitet {
    val nullableFørsteDagIInneværendeInaktivePeriode =
        if (førsteDagIInneværendeInaktivePeriode == LocalDate.MIN || førsteDagIInneværendeInaktivePeriode == LocalDate.MAX) null
        else førsteDagIInneværendeInaktivePeriode

    val sluttdatoerForInaktivePerioder = inaktivePerioder.map { it.sisteDag }

    return PerioderMedInaktivitet(nullableFørsteDagIInneværendeInaktivePeriode, sluttdatoerForInaktivePerioder)
}


private fun LocalDate.isAfterOrEqual(other: LocalDate): Boolean =
    this.isAfter(other) || this == other


private fun LocalDate.isBeforeOrEqual(other: LocalDate): Boolean =
    this.isBefore(other) || this == other


private fun LocalDate.plusOneDayOrMax(): LocalDate =
    if (this == LocalDate.MAX) LocalDate.MAX else plusDays(1)


private fun LocalDate.minusOneDayOrMin(): LocalDate =
    if (this == LocalDate.MIN) LocalDate.MIN else minusDays(1)


private fun <T> List<T>.headTail() =
    Pair(first(), subList(1, size))

