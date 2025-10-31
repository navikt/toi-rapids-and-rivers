package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import no.nav.toi.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class HullCvLytterTest {

    val nowWithDayOne = LocalDate.now().withDayOfMonth(1)

    @Test
    fun `legg på et eller annet svar om første behov er hullICv`() {
        val testRapid = TestRapid()
        startApp(testRapid)

        val melding = behovsMelding(
            behovListe = """["hullICv"]""",
            fødselsDato = LocalDate.now().minusYears(30)
        )
        testRapid.sendTestMessage(
            melding
        )

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
    }

    @Test
    fun `ikke legg på svar om andre behov enn hullICv som ikke er løst`() {
        val testRapid = TestRapid()
        startApp(testRapid)

        testRapid.sendTestMessage(
            behovsMelding(
                behovListe = """["noeannet", "hullICv"]""",
                fødselsDato = LocalDate.now().minusYears(30)
            )
        )

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `legg på svar om behov nummer 2 er hullICv, dersom første behov har en løsning`() {
        val testRapid = TestRapid()
        startApp(testRapid)

        testRapid.sendTestMessage(
            behovsMelding(
                behovListe = """["noeannet", "hullICv"]""",
                løsninger = listOf("noeannet" to """{"noeannetsvar": 123}"""),
                fødselsDato = LocalDate.now().minusYears(30)
            )
        )

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
        val melding = inspektør.message(0)
        assertThat(melding["hullICv"].isMissingOrNull()).isFalse
        assertThat(melding["noeannet"]["noeannetsvar"].asInt()).isEqualTo(123)
    }

    @Test
    fun `legg på svar om behov nummer 2 er hullICv, dersom første behov har en løsning med null-verdi`() {
        val testRapid = TestRapid()
        startApp(testRapid)

        testRapid.sendTestMessage(
            behovsMelding(
                behovListe = """["noeannet", "hullICv"]""",
                løsninger = listOf("noeannet" to "null"),
                fødselsDato = LocalDate.now().minusYears(30)
            )
        )

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
        val melding = inspektør.message(0)
        assertThat(melding["hullICv"].isMissingOrNull()).isFalse
        assertThat(melding["noeannet"].isNull).isTrue
    }

    @Test
    fun `ikke legg på svar om svar allerede er lagt på med null-verdi`() {
        val testRapid = TestRapid()
        startApp(testRapid)

        testRapid.sendTestMessage(
            behovsMelding(
                behovListe = """["hullICv"]""",
                løsninger = listOf("hullICv" to "null"),
                fødselsDato = LocalDate.now().minusYears(30)
            )
        )

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `ikke legg på svar om behov er en tom liste`() {
        val testRapid = TestRapid()
        startApp(testRapid)

        testRapid.sendTestMessage(behovsMelding(behovListe = "[]", fødselsDato = LocalDate.now().minusYears(30)))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `ikke legg på svar om svar allerede er lagt på`() {
        val testRapid = TestRapid()
        startApp(testRapid)

        testRapid.sendTestMessage(
            behovsMelding(
                behovListe = """["hullICv"]""",
                løsninger = listOf("hullICv" to """"svar""""),
                fødselsDato = LocalDate.now().minusYears(30)
            )
        )

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun cvUtenYrkeserfaringOgUtdanning() {
        val melding = lagBehovmeldingMedErfaring()

        assertThat(melding.sluttdatoerForInaktivePerioder).isEmpty()
        assertThat(melding.førsteDagIInneværendeInaktivePeriode).isNull()
    }

    @Test
    fun cvMedÉnPågåendeYrkeserfaring() {
        val startdato = nowWithDayOne.minusYears(10)
        val melding = lagBehovmeldingMedErfaring(arbeidserfaring = listOf(CVPeriode(startdato, null)))
        assertThat(melding.sluttdatoerForInaktivePerioder)
            .containsExactly(startdato.minusDays(1))
        assertThat(melding.førsteDagIInneværendeInaktivePeriode).isNull()
    }

    @Test
    fun cvMedÉnAvsluttetYrkeserfaring() {
        val startdatoForErfaring = nowWithDayOne.minusYears(10)
        val sluttdatoForErfaring = nowWithDayOne.minusYears(1)
        val melding = lagBehovmeldingMedErfaring(
            arbeidserfaring = listOf(
                CVPeriode(
                    startdatoForErfaring,
                    sluttdatoForErfaring
                )
            )
        )

        assertThat(melding.sluttdatoerForInaktivePerioder)
            .containsExactly(startdatoForErfaring.minusDays(1))
        assertThat(melding.førsteDagIInneværendeInaktivePeriode)
            .isEqualTo(sluttdatoForErfaring.plusDays(1))
    }

    @Test
    fun cvMedOverlappendeYrkeserfaring() {
        val tidligsteStartdato = nowWithDayOne.minusYears(10)
        val tidligsteSluttdato = nowWithDayOne.minusYears(3)
        val senesteStartdato = nowWithDayOne.minusYears(9)
        val senesteSluttdato = nowWithDayOne.minusYears(1)

        val melding = lagBehovmeldingMedErfaring(
            arbeidserfaring = listOf(
                CVPeriode(tidligsteStartdato, tidligsteSluttdato),
                CVPeriode(senesteStartdato, senesteSluttdato),
            )
        )

        assertThat(melding.sluttdatoerForInaktivePerioder)
            .containsExactly(tidligsteStartdato.minusDays(1))
        assertThat(melding.førsteDagIInneværendeInaktivePeriode)
            .isEqualTo(senesteSluttdato.plusDays(1))
    }

    @Test
    fun cvMedÉnAvsluttetUtdanningOgIkkeIAktivitetNå() {
        val startdato = nowWithDayOne.minusYears(6)
        val sluttdato = nowWithDayOne.minusYears(3)
        val melding =
            lagBehovmeldingMedErfaring(utdannelse = listOf(CVPeriode(startdato, sluttdato)))

        assertThat(melding.sluttdatoerForInaktivePerioder)
            .containsExactly(startdato.minusDays(1))
        assertThat(melding.førsteDagIInneværendeInaktivePeriode)
            .isEqualTo(sluttdato.plusDays(1))
    }

    @Test
    fun cvMedHullIMidten() {
        val startdato1 = nowWithDayOne.minusYears(10)
        val sluttdato1 = nowWithDayOne.minusYears(7)
        val startdato2 = nowWithDayOne.minusYears(4)
        val sluttDatoAsLocalDate = nowWithDayOne.minusYears(1)
        val sluttdato2 = sluttDatoAsLocalDate
        val erfaring = CVPeriode(startdato1, sluttdato1)
        val utdannelse = CVPeriode(startdato2, sluttdato2)
        val melding = lagBehovmeldingMedErfaring(
            arbeidserfaring = listOf(erfaring),
            utdannelse = listOf(utdannelse)
        )

        val expectedSluttdato1 = startdato1.minusDays(1)
        val expectedSluttdato2 = startdato2.minusDays(1)

        assertThat(melding.sluttdatoerForInaktivePerioder).containsExactly(expectedSluttdato1, expectedSluttdato2)
        assertThat(melding.førsteDagIInneværendeInaktivePeriode).isEqualTo(sluttDatoAsLocalDate.plusDays(1))
    }

    @Test
    fun cvMedHullIMidtenMindreEnnToÅr() {
        val startdato1 = nowWithDayOne.minusYears(10)
        val sluttdato1 = nowWithDayOne.minusYears(7)
        val startdato2 = nowWithDayOne.minusYears(5).minusDays(1)
        val sluttdato2 = nowWithDayOne.minusYears(1)
        val melding = lagBehovmeldingMedErfaring(
            arbeidserfaring = listOf(CVPeriode(startdato1, sluttdato1)),
            utdannelse = listOf(CVPeriode(startdato2, sluttdato2))
        )
        assertThat(melding.sluttdatoerForInaktivePerioder).containsExactly(startdato1.minusDays(1))
        assertThat(melding.førsteDagIInneværendeInaktivePeriode)
            .isEqualTo(sluttdato2.plusDays(1))
    }

    @Test
    fun cvMedToHullIMidten() {
        val startdato1 = nowWithDayOne.minusYears(16)
        val sluttdato1 = nowWithDayOne.minusYears(13)
        val startdato2 = nowWithDayOne.minusYears(10)
        val sluttdato2 = nowWithDayOne.minusYears(7)
        val startdato3 = nowWithDayOne.minusYears(4)
        val sluttdato3 = nowWithDayOne.minusYears(1)

        val melding = lagBehovmeldingMedErfaring(
            arbeidserfaring = listOf(CVPeriode(startdato1, sluttdato1)),
            utdannelse = listOf(
                CVPeriode(startdato2, sluttdato2),
                CVPeriode(startdato3, sluttdato3),
            ),
        )

        assertThat(melding.sluttdatoerForInaktivePerioder)
            .containsExactly(startdato1.minusDays(1), startdato2.minusDays(1), startdato3.minusDays(1))
        assertThat(melding.førsteDagIInneværendeInaktivePeriode).isEqualTo(sluttdato3.plusDays(1))
    }

    @Test
    fun cvMedEnYrkeserfaringSomOmslutterEnAnnenYrkeserfaring() {
        val startdato1 = nowWithDayOne.minusYears(15)
        val sluttdato1 = nowWithDayOne.minusYears(2)
        val startdato2 = nowWithDayOne.minusYears(14)
        val sluttdato2 = nowWithDayOne.minusYears(4)

        val melding = lagBehovmeldingMedErfaring(
            arbeidserfaring = listOf(
                CVPeriode(startdato1, sluttdato1),
                CVPeriode(startdato2, sluttdato2),
            ),
        )
        assertThat(melding.sluttdatoerForInaktivePerioder).containsExactly(startdato1.minusDays(1))
        assertThat(melding.førsteDagIInneværendeInaktivePeriode).isEqualTo(sluttdato1.plusDays(1))
    }

    @Test
    fun cvMedToYrkeserfaringerSomStarterSamtidigMenAvsluttesPåUlikDato() {
        val startdato1 = nowWithDayOne.minusYears(10)
        val sluttdato1 = nowWithDayOne.minusYears(2)
        val startdato2 = nowWithDayOne.minusYears(10)
        val sluttdato2 = nowWithDayOne.minusYears(8)

        val meldingLengsteErfaringFørst = lagBehovmeldingMedErfaring(
            arbeidserfaring = listOf(
                CVPeriode(startdato1, sluttdato1),
                CVPeriode(startdato2, sluttdato2),
            ),
        )

        val meldingKortesteErfaringFørst = lagBehovmeldingMedErfaring(
            arbeidserfaring = listOf(
                CVPeriode(startdato2, sluttdato2),
                CVPeriode(startdato1, sluttdato1)
            ),
        )

        assertThat(meldingLengsteErfaringFørst.sluttdatoerForInaktivePerioder).containsExactly(startdato1.minusDays(1))
        assertThat(meldingLengsteErfaringFørst.førsteDagIInneværendeInaktivePeriode).isEqualTo(sluttdato1.plusDays(1))
        assertThat(meldingKortesteErfaringFørst.asText()).isEqualTo(meldingLengsteErfaringFørst.asText())
    }

    @Test
    fun harHattJobbTidligereOgHarEnPågåendeJobb() {
        val startdato1 = nowWithDayOne.minusYears(10)
        val sluttdato1 = nowWithDayOne.minusYears(6)
        val startdato2 = nowWithDayOne.minusYears(2)
        val sluttdato2: LocalDate? = null

        val melding = lagBehovmeldingMedErfaring(
            arbeidserfaring = listOf(
                CVPeriode(startdato1, sluttdato1),
                CVPeriode(startdato2, sluttdato2),
            ),
        )

        assertThat(melding.sluttdatoerForInaktivePerioder)
            .containsExactlyInAnyOrder(startdato1.minusDays(1), startdato2.minusDays(1))
        assertThat(melding.førsteDagIInneværendeInaktivePeriode).isNull()
    }

    @Test
    fun flereYrkeserfaringerSomStarterPåUlikDatoOgSlutterPåSammeDato() {
        val sammeSluttdato = nowWithDayOne.minusYears(4)
        val startdato1 = nowWithDayOne.minusYears(10)
        val startdato2 = nowWithDayOne.minusYears(9)
        val startdato3 = nowWithDayOne.minusYears(8)

        val melding = lagBehovmeldingMedErfaring(
            arbeidserfaring = listOf(
                CVPeriode(startdato1, sammeSluttdato),
                CVPeriode(startdato2, sammeSluttdato),
                CVPeriode(startdato3, sammeSluttdato),
            ),
        )

        assertThat(melding.sluttdatoerForInaktivePerioder).containsExactly(startdato1.minusDays(1))
        assertThat(melding.førsteDagIInneværendeInaktivePeriode).isEqualTo(sammeSluttdato.plusDays(1))
    }

    @Test
    fun ignorerPerioderSomStarterEtterEnPeriodeUtenSluttdato() {
        val startdato1 = nowWithDayOne.minusYears(10)
        val sluttdato1 = nowWithDayOne.minusYears(8)
        val startdato2 = nowWithDayOne.minusYears(5)
        val sluttdato2: LocalDate? = null
        val startdato3 = nowWithDayOne.minusYears(3)
        val sluttdato3 = nowWithDayOne.minusYears(2)

        val melding = lagBehovmeldingMedErfaring(
            arbeidserfaring = listOf(
                CVPeriode(startdato1, sluttdato1),
                CVPeriode(startdato2, sluttdato2),
                CVPeriode(startdato3, sluttdato3),
            ),
        )

        assertThat(melding.sluttdatoerForInaktivePerioder)
            .containsExactly(startdato1.minusDays(1), startdato2.minusDays(1))
        assertThat(melding.førsteDagIInneværendeInaktivePeriode).isNull()
    }

    @Test
    fun rekkefølgeSpillerIngenRolleForToYrkeserfaringerSomStarterSamtidigDenEneUtenSluttdato() {
        val startdato1 = nowWithDayOne.minusYears(5)
        val sluttdato1 = nowWithDayOne.minusYears(2)
        val sluttdatoTom: LocalDate? = null

        val meldingTomSluttdatoSist = lagBehovmeldingMedErfaring(
            arbeidserfaring = listOf(
                CVPeriode(startdato1, sluttdato1),
                CVPeriode(startdato1, sluttdatoTom),
            ),
        )

        val meldingTomSluttdatoFørst = lagBehovmeldingMedErfaring(
            arbeidserfaring = listOf(
                CVPeriode(startdato1, sluttdatoTom),
                CVPeriode(startdato1, sluttdato1),
            ),
        )

        assertThat(meldingTomSluttdatoSist.sluttdatoerForInaktivePerioder).containsExactly(startdato1.minusDays(1))
        assertThat(meldingTomSluttdatoSist.førsteDagIInneværendeInaktivePeriode).isNull()
        assertThat(meldingTomSluttdatoSist.asText()).isEqualTo(meldingTomSluttdatoFørst.asText())
    }

    @Test
    fun enAvsluttetErfaringUtenStartOgSluttdato() {
        val startdato1: LocalDate? = null
        val sluttdato1: LocalDate? = null

        val melding = lagBehovmeldingMedErfaring(
            arbeidserfaring = listOf(
                CVPeriode(startdato1, sluttdato1),
            ),
        )

        assertThat(melding.sluttdatoerForInaktivePerioder).isEmpty()
        assertThat(melding.førsteDagIInneværendeInaktivePeriode).isNull()
    }

    @Test
    fun enAvsluttetErfaringUtenStartdato() {
        val startdato1: LocalDate? = null
        val sluttdato1 = nowWithDayOne.minusYears(3)

        val melding = lagBehovmeldingMedErfaring(
            arbeidserfaring = listOf(
                CVPeriode(startdato1, sluttdato1),
            ),
        )
        assertThat(melding.sluttdatoerForInaktivePerioder).isEmpty()
        assertThat(melding.førsteDagIInneværendeInaktivePeriode)
            .isEqualTo(sluttdato1.plusDays(1))
    }

    @Test
    fun flereYrkeserfaringerDerEnErUtenStartdato() {
        val startdato1 = nowWithDayOne.minusYears(11)
        val sluttdato1 = nowWithDayOne.minusYears(9)
        val startdato2: LocalDate? = null
        val sluttdato2 = nowWithDayOne.minusYears(5)
        val startdato3 = nowWithDayOne.minusYears(2)
        val sluttdato3 = nowWithDayOne.minusYears(1)

        val melding = lagBehovmeldingMedErfaring(
            arbeidserfaring = listOf(
                CVPeriode(startdato1, sluttdato1),
                CVPeriode(startdato2, sluttdato2),
                CVPeriode(startdato3, sluttdato3),
            ),
        )

        assertThat(melding.sluttdatoerForInaktivePerioder).containsExactly(startdato3.minusDays(1))
        assertThat(melding.førsteDagIInneværendeInaktivePeriode).isEqualTo(sluttdato3.plusDays(1))
    }

    @Test
    fun spesiellPerson() {
        val yrkeserfaringer = listOf(
            "2017-10-01" to "2021-04-01",
            "2016-01-01" to "2017-10-01",
            "2009-07-01" to "2012-12-01",
            "2009-01-01" to "2014-03-01",
            "2006-10-01" to "2016-01-01",
            "2004-05-01" to "2006-11-01",
            "2003-06-01" to "2004-05-01",
            "2001-12-01" to "2003-06-01",
            "1999-04-01" to "2000-11-01"
        ).map { LocalDate.parse(it.first) to LocalDate.parse(it.second) }
            .map { CVPeriode(it.first,it.second) }
        val utdanninger = listOf(
            "2013-07-01" to "2014-05-01",
            "1999-07-01" to "2000-05-01",
            "1995-07-01" to "1998-05-01",
            "1985-07-01" to "1995-05-01"
        ).map { LocalDate.parse(it.first) to LocalDate.parse(it.second) }
            .map { CVPeriode(it.first,it.second) }

        val melding = lagBehovmeldingMedErfaring(
            arbeidserfaring = yrkeserfaringer,
            utdannelse = utdanninger,
            fødselsDato = LocalDate.of(1984,2,16)
        )

        assertThat(melding.sluttdatoerForInaktivePerioder).isEmpty()
        assertThat(melding.førsteDagIInneværendeInaktivePeriode).isEqualTo(LocalDate.of(2021,4,2))
    }

    @Test
    fun skalIgnorereAvsluttetInaktivitetOppTilEnVissAlder_gittAktivperiodeSlutterFørDenVisseAlderen() {
        val fødselsdato = LocalDate.of(1989, 6, 20)
        val aktivFraOgMed = LocalDate.of(fødselsdato.year + 6, 8, 14)
        val aktivTilOgMed = LocalDate.of(fødselsdato.year + 17, 9, 1)

        val melding = lagBehovmeldingMedErfaring(
            utdannelse = listOf(
                CVPeriode(aktivFraOgMed, aktivTilOgMed),
            ),
            fødselsDato = fødselsdato
        )
        assertThat(melding.sluttdatoerForInaktivePerioder).isEmpty()
        assertThat(melding.førsteDagIInneværendeInaktivePeriode).isEqualTo(aktivTilOgMed.plusDays(1))
    }

    @Test
    fun skalIgnorereAvsluttedeInaktiviteterOppTilEnVissAlder_gittAktivperioderSlutterFørDenVisseAlderen() {
        val fødselsdato = LocalDate.of(1989, 6, 20)
        val aktivFraOgMed1 = LocalDate.of(fødselsdato.year + 3, 8, 14)
        val aktivTilOgMed1 = LocalDate.of(fødselsdato.year + 5, 9, 1)
        val aktivFraOgMed2 = LocalDate.of(fødselsdato.year + 8, 8, 14)
        val aktivTilOgMed2 = LocalDate.of(fødselsdato.year + 10, 9, 1)

        val melding = lagBehovmeldingMedErfaring(
            utdannelse = listOf(
                CVPeriode(aktivFraOgMed1, aktivTilOgMed1),
                CVPeriode(aktivFraOgMed2, aktivTilOgMed2),
            ),
            fødselsDato = fødselsdato
        )

        assertThat(melding.sluttdatoerForInaktivePerioder).isEmpty()
        assertThat(melding.førsteDagIInneværendeInaktivePeriode).isEqualTo(aktivTilOgMed2.plusDays(1))
    }

    @Test
    fun skalIgnorereAvsluttetInaktivitetOppTilEnVissAlder_gittAktivperiodeSlutterEtterDenVisseAlderen() {
        val fødselsdato = LocalDate.of(1989, 6, 20)
        val aktivFraOgMed = LocalDate.of(fødselsdato.year + 17, 8, 1)
        val aktivTilOgMed = LocalDate.of(aktivFraOgMed.year + 3, 6, 1)

        val melding = lagBehovmeldingMedErfaring(
            utdannelse = listOf(
                CVPeriode(aktivFraOgMed, aktivTilOgMed)
            ),
            fødselsDato = fødselsdato
        )
        assertThat(melding.sluttdatoerForInaktivePerioder).isEmpty()
        assertThat(melding.førsteDagIInneværendeInaktivePeriode).isEqualTo(aktivTilOgMed.plusDays(1))
    }

    @Test
    fun skalIkkeIgnorereAvsluttetInaktivitetEtterEnVissAlder() {
        val fødselsdato = LocalDate.of(1989, 6, 20)

        val aktivFraOgMed = LocalDate.of(fødselsdato.year + 17, 9, 1)
        val aktivTilOgMed = LocalDate.of(aktivFraOgMed.year + 3, 6, 1)

        val melding = lagBehovmeldingMedErfaring(
            utdannelse = listOf(
                CVPeriode(aktivFraOgMed, aktivTilOgMed)
            ),
            fødselsDato = fødselsdato
        )
        assertThat(melding.sluttdatoerForInaktivePerioder).containsExactly(aktivFraOgMed.minusDays(1))
        assertThat(melding.førsteDagIInneværendeInaktivePeriode).isEqualTo(aktivTilOgMed.plusDays(1))
    }

    fun lagBehovmeldingMedErfaring(
        arbeidserfaring: List<CVPeriode> = emptyList(),
        utdannelse: List<CVPeriode> = emptyList(),
        fødselsDato: LocalDate = LocalDate.parse("1960-01-01")
    ): JsonNode {
        val behovsmelding = behovsMelding(
            behovListe = """["hullICv"]""",
            arbeidserfaring = arbeidserfaring,
            utdannelse = utdannelse,
            fødselsDato = fødselsDato
        )

        val testRapid = TestRapid()
        startApp(testRapid)
        testRapid.sendTestMessage(behovsmelding)

        val inspektør = testRapid.inspektør
        assertThat(inspektør.size).isEqualTo(1)
        return inspektør.message(0)
    }


    private fun behovsMelding(
        behovListe: String,
        løsninger: List<Pair<String, String>> = emptyList(),
        utdannelse: List<CVPeriode> = emptyList(),
        arbeidserfaring: List<CVPeriode> = emptyList(),
        fødselsDato: LocalDate
    ) = """
        {
            "aktørId":"123",
            "@behov":$behovListe,
            "arbeidsmarkedCv": {
                "opprettCv": {
                    "cv": {
                        "utdannelse": ${utdannelse.map { it.tilArbeidsmarkedJson()}.joinToString(",", "[", "]")},
                        "arbeidserfaring": ${arbeidserfaring.map { it.tilArbeidsmarkedJson()}.joinToString(",", "[", "]")},
                        "foedselsdato": ${fødselsDato.tilIntList().tilJsonString()}
                    }
                }
            }
            ${løsninger.joinToString() { ""","${it.first}":${it.second}""" }}
        }
    """.trimIndent()

    private val objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    private fun List<Any>.tilJsonString() = joinToString(
        prefix = "[",
        postfix = "]",
        transform = objectMapper::writeValueAsString
    )

    private val JsonNode.sluttdatoerForInaktivePerioder get() = this["hullICv"]["sluttdatoerForInaktivePerioder"].map { it.asLocalDate() }
    private val JsonNode.førsteDagIInneværendeInaktivePeriode
        get() = this["hullICv"]["førsteDagIInneværendeInaktivePeriode"].let {
            if (it.isNull) null else it.asLocalDate()
        }

    private fun LocalDate.tilIntList() = listOf(year, monthValue, dayOfMonth)
}
