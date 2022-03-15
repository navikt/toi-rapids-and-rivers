package no.nav.arbeidsgiver.toi.hullicv

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

class HullCvLytterTest {

    @Test
    fun `legg på et eller annet svar om første behov er hullICv`() {
        val testRapid = TestRapid()
        startApp(testRapid)

        testRapid.sendTestMessage(behovsMelding(behovListe = """["hullICv"]""",fødselsDato = LocalDate.now().minusYears(30)))

        val inspektør = testRapid.inspektør

        Assertions.assertThat(inspektør.size).isEqualTo(1)
    }

    @Test
    fun `ikke legg på svar om andre behov enn hullICv som ikke er løst`() {
        val testRapid = TestRapid()
        startApp(testRapid)

        testRapid.sendTestMessage(behovsMelding(behovListe = """["noeannet", "hullICv"]""",fødselsDato = LocalDate.now().minusYears(30)))

        val inspektør = testRapid.inspektør

        Assertions.assertThat(inspektør.size).isEqualTo(0)
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

        Assertions.assertThat(inspektør.size).isEqualTo(1)
        val melding = inspektør.message(0)
        Assertions.assertThat(melding["hullICv"].isMissingOrNull()).isFalse
        Assertions.assertThat(melding["noeannet"]["noeannetsvar"].asInt()).isEqualTo(123)
    }

    @Test
    fun `ikke legg på svar om behov er en tom liste`() {
        val testRapid = TestRapid()
        startApp(testRapid)

        testRapid.sendTestMessage(behovsMelding(behovListe = "[]",fødselsDato = LocalDate.now().minusYears(30)))

        val inspektør = testRapid.inspektør

        Assertions.assertThat(inspektør.size).isEqualTo(0)
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

        Assertions.assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun cvMedHullIMidten() {
        val startdato1 = LocalDate.now().minusYears(10)
        val sluttdato1 = LocalDate.now().minusYears(7).toEpochSecond(LocalTime.of(0,0,0), ZoneOffset.UTC) * 1000
        val startdato2 = LocalDate.now().minusYears(4)
        val sluttDatoAsLocalDate = LocalDate.now().minusYears(1)
        val sluttdato2 = sluttDatoAsLocalDate.toEpochSecond(LocalTime.of(0,0,0), ZoneOffset.UTC) * 1000
        val erfaring = CVPeriode(startdato1, sluttdato1)
        val utdannelse = CVPeriode(startdato2, sluttdato2)
        val behovsmelding = behovsMelding(behovListe = """["hullICv"]""", arbeidserfaring = listOf(erfaring), utdannelse = listOf(utdannelse),fødselsDato = LocalDate.now().minusYears(30))
        val testRapid = TestRapid()
        startApp(testRapid)
        testRapid.sendTestMessage(behovsmelding)

        val inspektør = testRapid.inspektør
        Assertions.assertThat(inspektør.size).isEqualTo(1)
        val melding = inspektør.message(0)

        val expectedSluttdato1 = startdato1.minusDays(1)
        val expectedSluttdato2 = startdato2.minusDays(1)

        Assertions.assertThat(melding["hullICv"]["sluttdatoerForInaktivePerioder"]
            .map(JsonNode::asLocalDate))
            .containsExactly(expectedSluttdato1, expectedSluttdato2)
        Assertions.assertThat(melding["hullICv"]["førsteDagIInneværendeInaktivePeriode"].asLocalDate())
            .isEqualTo(sluttDatoAsLocalDate.plusDays(1))
    }

    /*@Test
    fun cvMedHullIMidtenMindreEnnToÅr() {
        val startdato1 = LocalDate.now().minusYears(10)
        val sluttdato1 = LocalDate.now().minusYears(7)
        val startdato2 = LocalDate.now().minusYears(5).minusDays(1)
        val sluttdato2 = LocalDate.now().minusYears(1)
        val erfaring = lagErfaring(startdato1, sluttdato1)
        val utdannelse = lagUtdannelse(startdato2, sluttdato2)
        val cvEvent = CvEventObjectMother.giveMeCvEvent(listOf(erfaring), listOf(utdannelse))
        val actual = esCvTransformer.transform(cvEvent.tilSammenstiltKandidat()).perioderMedInaktivitet
        val expectedSluttdato = CvEventObjectMother.safeToDate(startdato1.minusDays(1))
        assertThat(actual.sluttdatoerForInaktivePerioderPaToArEllerMer).containsExactly(expectedSluttdato)
        assertThat(actual.startdatoForInnevarendeInaktivePeriode)
            .isEqualTo(CvEventObjectMother.safeToDate(sluttdato2.plusDays(1)))
    }

    @Test
    fun cvMedToHullIMidten() {
        val startdato1 = LocalDate.now().minusYears(16)
        val sluttdato1 = LocalDate.now().minusYears(13)
        val startdato2 = LocalDate.now().minusYears(10)
        val sluttdato2 = LocalDate.now().minusYears(7)
        val startdato3 = LocalDate.now().minusYears(4)
        val sluttdato3 = LocalDate.now().minusYears(1)
        val erfaring = lagErfaring(startdato1, sluttdato1)
        val utdannelse = lagUtdannelse(startdato2, sluttdato2)
        val utdannelse2 = lagUtdannelse(startdato3, sluttdato3)
        val cvEvent = CvEventObjectMother.giveMeCvEvent(listOf(erfaring), Arrays.asList(utdannelse, utdannelse2))
        val actual = esCvTransformer.transform(cvEvent.tilSammenstiltKandidat()).perioderMedInaktivitet
        val expectedSluttdato1 = CvEventObjectMother.safeToDate(startdato1.minusDays(1))
        val expectedSluttdato2 = CvEventObjectMother.safeToDate(startdato2.minusDays(1))
        val expectedSluttdato3 = CvEventObjectMother.safeToDate(startdato3.minusDays(1))
        val expectedSluttdato4 = CvEventObjectMother.safeToDate(sluttdato3.plusDays(1))
        assertThat(actual.sluttdatoerForInaktivePerioderPaToArEllerMer)
            .containsExactly(expectedSluttdato1, expectedSluttdato2, expectedSluttdato3)
        assertThat(actual.startdatoForInnevarendeInaktivePeriode).isEqualTo(expectedSluttdato4)
    }*/


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
            "cv": {
                "opprettCv": {
                    "cv": {
                        "uviktigfelt": true,
                        "utdannelse": ${utdannelse.tilJsonString()},
                        "arbeidserfaring": ${arbeidserfaring.tilJsonString()},
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
}

private fun LocalDate.tilIntList() = listOf(year,monthValue,dayOfMonth)
