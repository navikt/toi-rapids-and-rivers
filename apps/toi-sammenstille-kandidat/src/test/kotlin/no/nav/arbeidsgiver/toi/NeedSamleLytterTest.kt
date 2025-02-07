package no.nav.arbeidsgiver.toi

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.javalin.Javalin
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class NeedSamleLytterTest {

    private lateinit var javalin: Javalin

    val testDatabase = TestDatabase()

    @BeforeEach
    fun before() {
        javalin = Javalin.create().start(9000)

        testDatabase.lagreKandidat(
            Kandidat.fraJson(
                """
                {
                    "aktørId": "$aktørId",
                    "arbeidsmarkedCv": "$arbeidsmarkedCv",
                    "veileder": "$veileder",
                    "oppfølgingsinformasjon": "$oppfølgingsinformasjon",
                    "siste14avedtak": "$siste14avedtak",
                    "oppfølgingsperiode": "$oppfølgingsperiode",
                    "arenaFritattKandidatsøk": "$arenaFritattKandidatsøk",
                    "hjemmel": "$hjemmel",
                    "måBehandleTidligereCv": "$måBehandleTidligereCv",
                    "kvp": "$kvp"
                }
            """.trimIndent()
            )
        )

        testDatabase.lagreKandidat(
            Kandidat.fraJson(
                """
                {
                    "aktørId": "$aktørIdUtenFelter"
                }
            """.trimIndent()
            )
        )
    }

    @AfterEach
    fun after() {
        javalin.stop()
    }

    companion object {
        val aktørId = "123123123"
        val aktørIdUtenFelter = "321321321"
        val aktørIdIkkeEksisterende = "000000"
        val arbeidsmarkedCv = "Gyldig arbeidsmarked-cv-svar"
        val veileder = "Gyldig veileder-svar"
        val oppfølgingsinformasjon = "Gyldig oppfølgingsinformasjon-svar"
        val siste14avedtak = "Gyldig svar på siste 14 a-status"
        val oppfølgingsperiode = "Gyldig oppfølgingsperiode-svar"
        val arenaFritattKandidatsøk = "Gyldif arena fritatt kandidatsøk-status"
        val hjemmel = "Gyldig hjemmel-svar"
        val måBehandleTidligereCv = "Gyldig status på må behandle tidligere cv"
        val kvp = "Gyldig kvp-status"
        @JvmStatic
        private fun felter() = listOf(
            Arguments.of("arbeidsmarkedCv", arbeidsmarkedCv),
            Arguments.of("veileder", veileder),
            Arguments.of("oppfølgingsinformasjon", oppfølgingsinformasjon),
            Arguments.of("siste14avedtak", siste14avedtak),
            Arguments.of("oppfølgingsperiode", oppfølgingsperiode),
            Arguments.of("arenaFritattKandidatsøk", arenaFritattKandidatsøk),
            Arguments.of("hjemmel", hjemmel),
            Arguments.of("måBehandleTidligereCv", måBehandleTidligereCv),
            Arguments.of("kvp", kvp)
        ).stream()
    }

    @ParameterizedTest
    @MethodSource("felter")
    fun `legg på svar om første behov er et gitt felt`(felt: String, expectedVerdi: String) {
        val testRapid = TestRapid()
        startApp(testRapid, testDatabase.dataSource, javalin, "dummy")

        testRapid.sendTestMessage(behovsMelding(ident = aktørId, behovListe = """["$felt"]"""))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
        val message = inspektør.message(0)
        assertThat(message[felt].asText()).isEqualTo(expectedVerdi)
    }

    @Test
    fun `ikke legg på svar om andre uløste behov enn veileder er først i listen`() {
        val testRapid = TestRapid()
        startApp(testRapid, testDatabase.dataSource, javalin, "dummy")

        testRapid.sendTestMessage(behovsMelding(ident = aktørId, behovListe = """["noeannet", "$felt"]"""))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(0)
    }

    @ParameterizedTest
    @MethodSource("felter")
    fun `legg på svar om behov nummer 2 er et gitt felt, dersom første behov har en løsning`(felt: String, expectedVerdi: String) {
        val testRapid = TestRapid()
        startApp(testRapid, testDatabase.dataSource, javalin, "dummy")

        testRapid.sendTestMessage(
            behovsMelding(
                ident = aktørId,
                behovListe = """["noeannet", "$felt"]""",
                løsninger = listOf("noeannet" to """{"noeannetsvar": 123}""")
            )
        )

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
        val melding = inspektør.message(0)
        assertThat(melding[felt].asText()).isEqualTo(expectedVerdi)
        assertThat(melding["noeannet"]["noeannetsvar"].asInt()).isEqualTo(123)
    }

    @ParameterizedTest
    @MethodSource("felter")
    fun `ikke legg på svar om svar allerede er lagt på`(felt: String, ikkeibruk: String) {
        val testRapid = TestRapid()
        startApp(testRapid, testDatabase.dataSource, javalin, "dummy")

        testRapid.sendTestMessage(
            behovsMelding(
                behovListe = """["$felt"]""",
                løsninger = listOf(felt to """"svar"""")
            )
        )

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `ikke legg på svar om behov er en tom liste`() {
        val testRapid = TestRapid()
        startApp(testRapid, testDatabase.dataSource, javalin, "dummy")

        testRapid.sendTestMessage(behovsMelding(behovListe = "[]"))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(0)
    }


}

private fun behovsMelding(
    ident: String = "12312312312",
    behovListe: String,
    løsninger: List<Pair<String, String>> = emptyList(),
) = """
        {
            "aktørId":"$ident",
            "@behov":$behovListe
            ${løsninger.joinToString { ""","${it.first}":${it.second}""" }}
        }
    """.trimIndent()