package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.javalin.Javalin
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class NeedSamleLytterTest {

    private lateinit var javalin: Javalin

    val aktørId = "123123123"
    val arbeidsmarkedCv = "Gyldig arbeidsmarked-cv-svar"
    val veileder = "Gyldig veileder-svar"
    val oppfølgingsinformasjon = "Gyldig oppfølgingsinformasjon-svar"
    val siste14avedtak = "Gyldig svar på siste 14 a-status"
    val oppfølgingsperiode = "Gyldig oppfølgingsperiode-svar"
    val arenaFritattKandidatsøk = "Gyldif arena fritatt kandidatsøk-status"
    val hjemmel = "Gyldig hjemmel-svar"
    val måBehandleTidligereCv = "Gyldig status på må behandle tidligere cv"
    val kvp = "Gyldig kvp-status"
    val testDatabase = TestDatabase()

    @BeforeEach
    fun before() {
        javalin = Javalin.create().start(9000)

        val kandidat = Kandidat.fraJson("""
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
        """.trimIndent())
        testDatabase.lagreKandidat(kandidat)
    }

    @AfterEach
    fun after() {
        javalin.stop()
    }

    @Test
    fun `legg på svar om første behov er veileder`() {
        val testRapid = TestRapid()
        startApp(testRapid, testDatabase.dataSource, javalin, "dummy")

        testRapid.sendTestMessage(behovsMelding(ident = aktørId, behovListe = """["veileder"]"""))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
        val message = inspektør.message(0)
        assertThat(message["veileder"].asText()).isEqualTo(veileder)
    }

    @Test
    fun `ikke legg på svar om andre uløste behov enn veileder er først i listen`() {
        val testRapid = TestRapid()
        startApp(testRapid, testDatabase.dataSource, javalin, "dummy")

        testRapid.sendTestMessage(behovsMelding(ident = aktørId, behovListe = """["noeannet", "veileder"]"""))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `legg på svar om behov nummer 2 er veileder, dersom første behov har en løsning`() {
        val testRapid = TestRapid()
        startApp(testRapid, testDatabase.dataSource, javalin, "dummy")

        testRapid.sendTestMessage(
            behovsMelding(
                ident = aktørId,
                behovListe = """["noeannet", "veileder"]""",
                løsninger = listOf("noeannet" to """{"noeannetsvar": 123}""")
            )
        )

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
        val melding = inspektør.message(0)
        assertThat(melding["veileder"].asText()).isEqualTo(veileder)
        assertThat(melding["noeannet"]["noeannetsvar"].asInt()).isEqualTo(123)
    }

    @Test
    fun `ikke legg på veileder-svar om svar allerede er lagt på`() {
        val testRapid = TestRapid()
        startApp(testRapid, testDatabase.dataSource, javalin, "dummy")

        testRapid.sendTestMessage(
            behovsMelding(
                behovListe = """["veileder"]""",
                løsninger = listOf("veileder" to """"svar"""")
            )
        )

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `legg på svar om første behov er oppfølgingsinformasjon`() {
        val testRapid = TestRapid()
        startApp(testRapid, testDatabase.dataSource, javalin, "dummy")

        testRapid.sendTestMessage(behovsMelding(ident = aktørId, behovListe = """["oppfølgingsinformasjon"]"""))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
        val message = inspektør.message(0)
        assertThat(message["oppfølgingsinformasjon"].asText()).isEqualTo(oppfølgingsinformasjon)
    }

    @Test
    fun `ikke legg på svar om andre uløste behov enn oppfølgingsinformasjon er først i listen`() {
        val testRapid = TestRapid()
        startApp(testRapid, testDatabase.dataSource, javalin, "dummy")

        testRapid.sendTestMessage(behovsMelding(ident = aktørId, behovListe = """["noeannet", "oppfølgingsinformasjon"]"""))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `legg på svar om behov nummer 2 er oppfølgingsinformasjon, dersom første behov har en løsning`() {
        val testRapid = TestRapid()
        startApp(testRapid, testDatabase.dataSource, javalin, "dummy")

        testRapid.sendTestMessage(
            behovsMelding(
                ident = aktørId,
                behovListe = """["noeannet", "oppfølgingsinformasjon"]""",
                løsninger = listOf("noeannet" to """{"noeannetsvar": 123}""")
            )
        )

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
        val melding = inspektør.message(0)
        assertThat(melding["oppfølgingsinformasjon"].asText()).isEqualTo(oppfølgingsinformasjon)
        assertThat(melding["noeannet"]["noeannetsvar"].asInt()).isEqualTo(123)
    }

    @Test
    fun `ikke legg på oppfølgingsinformasjon-svar om svar allerede er lagt på`() {
        val testRapid = TestRapid()
        startApp(testRapid, testDatabase.dataSource, javalin, "dummy")

        testRapid.sendTestMessage(
            behovsMelding(
                behovListe = """["oppfølgingsinformasjon"]""",
                løsninger = listOf("oppfølgingsinformasjon" to """"svar"""")
            )
        )

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `legg på svar om første behov er siste14avedtak`() {
        val testRapid = TestRapid()
        startApp(testRapid, testDatabase.dataSource, javalin, "dummy")

        testRapid.sendTestMessage(behovsMelding(ident = aktørId, behovListe = """["siste14avedtak"]"""))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
        val message = inspektør.message(0)
        assertThat(message["siste14avedtak"].asText()).isEqualTo(siste14avedtak)
    }

    @Test
    fun `ikke legg på svar om andre uløste behov enn siste14avedtak er først i listen`() {
        val testRapid = TestRapid()
        startApp(testRapid, testDatabase.dataSource, javalin, "dummy")

        testRapid.sendTestMessage(behovsMelding(ident = aktørId, behovListe = """["noeannet", "siste14avedtak"]"""))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `legg på svar om behov nummer 2 er siste14avedtak, dersom første behov har en løsning`() {
        val testRapid = TestRapid()
        startApp(testRapid, testDatabase.dataSource, javalin, "dummy")

        testRapid.sendTestMessage(
            behovsMelding(
                ident = aktørId,
                behovListe = """["noeannet", "siste14avedtak"]""",
                løsninger = listOf("noeannet" to """{"noeannetsvar": 123}""")
            )
        )

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
        val melding = inspektør.message(0)
        assertThat(melding["siste14avedtak"].asText()).isEqualTo(siste14avedtak)
        assertThat(melding["noeannet"]["noeannetsvar"].asInt()).isEqualTo(123)
    }

    @Test
    fun `ikke legg på siste14avedtak-svar om svar allerede er lagt på`() {
        val testRapid = TestRapid()
        startApp(testRapid, testDatabase.dataSource, javalin, "dummy")

        testRapid.sendTestMessage(
            behovsMelding(
                behovListe = """["siste14avedtak"]""",
                løsninger = listOf("siste14avedtak" to """"svar"""")
            )
        )

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `legg på svar om første behov er oppfølgingsperiode`() {
        val testRapid = TestRapid()
        startApp(testRapid, testDatabase.dataSource, javalin, "dummy")

        testRapid.sendTestMessage(behovsMelding(ident = aktørId, behovListe = """["oppfølgingsperiode"]"""))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
        val message = inspektør.message(0)
        assertThat(message["oppfølgingsperiode"].asText()).isEqualTo(oppfølgingsperiode)
    }

    @Test
    fun `ikke legg på svar om andre uløste behov enn oppfølgingsperiode er først i listen`() {
        val testRapid = TestRapid()
        startApp(testRapid, testDatabase.dataSource, javalin, "dummy")

        testRapid.sendTestMessage(behovsMelding(ident = aktørId, behovListe = """["noeannet", "oppfølgingsperiode"]"""))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `legg på svar om behov nummer 2 er oppfølgingsperiode, dersom første behov har en løsning`() {
        val testRapid = TestRapid()
        startApp(testRapid, testDatabase.dataSource, javalin, "dummy")

        testRapid.sendTestMessage(
            behovsMelding(
                ident = aktørId,
                behovListe = """["noeannet", "oppfølgingsperiode"]""",
                løsninger = listOf("noeannet" to """{"noeannetsvar": 123}""")
            )
        )

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
        val melding = inspektør.message(0)
        assertThat(melding["oppfølgingsperiode"].asText()).isEqualTo(oppfølgingsperiode)
        assertThat(melding["noeannet"]["noeannetsvar"].asInt()).isEqualTo(123)
    }

    @Test
    fun `ikke legg på oppfølgingsperiode-svar om svar allerede er lagt på`() {
        val testRapid = TestRapid()
        startApp(testRapid, testDatabase.dataSource, javalin, "dummy")

        testRapid.sendTestMessage(
            behovsMelding(
                behovListe = """["oppfølgingsperiode"]""",
                løsninger = listOf("oppfølgingsperiode" to """"svar"""")
            )
        )

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `legg på svar om første behov er arenaFritattKandidatsøk`() {
        val testRapid = TestRapid()
        startApp(testRapid, testDatabase.dataSource, javalin, "dummy")

        testRapid.sendTestMessage(behovsMelding(ident = aktørId, behovListe = """["arenaFritattKandidatsøk"]"""))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
        val message = inspektør.message(0)
        assertThat(message["arenaFritattKandidatsøk"].asText()).isEqualTo(arenaFritattKandidatsøk)
    }

    @Test
    fun `ikke legg på svar om andre uløste behov enn arenaFritattKandidatsøk er først i listen`() {
        val testRapid = TestRapid()
        startApp(testRapid, testDatabase.dataSource, javalin, "dummy")

        testRapid.sendTestMessage(behovsMelding(ident = aktørId, behovListe = """["noeannet", "arenaFritattKandidatsøk"]"""))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `legg på svar om behov nummer 2 er arenaFritattKandidatsøk, dersom første behov har en løsning`() {
        val testRapid = TestRapid()
        startApp(testRapid, testDatabase.dataSource, javalin, "dummy")

        testRapid.sendTestMessage(
            behovsMelding(
                ident = aktørId,
                behovListe = """["noeannet", "arenaFritattKandidatsøk"]""",
                løsninger = listOf("noeannet" to """{"noeannetsvar": 123}""")
            )
        )

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
        val melding = inspektør.message(0)
        assertThat(melding["arenaFritattKandidatsøk"].asText()).isEqualTo(arenaFritattKandidatsøk)
        assertThat(melding["noeannet"]["noeannetsvar"].asInt()).isEqualTo(123)
    }

    @Test
    fun `ikke legg på arenaFritattKandidatsøk-svar om svar allerede er lagt på`() {
        val testRapid = TestRapid()
        startApp(testRapid, testDatabase.dataSource, javalin, "dummy")

        testRapid.sendTestMessage(
            behovsMelding(
                behovListe = """["arenaFritattKandidatsøk"]""",
                løsninger = listOf("arenaFritattKandidatsøk" to """"svar"""")
            )
        )

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `legg på svar om første behov er hjemmel`() {
        val testRapid = TestRapid()
        startApp(testRapid, testDatabase.dataSource, javalin, "dummy")

        testRapid.sendTestMessage(behovsMelding(ident = aktørId, behovListe = """["hjemmel"]"""))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
        val message = inspektør.message(0)
        assertThat(message["hjemmel"].asText()).isEqualTo(hjemmel)
    }

    @Test
    fun `ikke legg på svar om andre uløste behov enn hjemmel er først i listen`() {
        val testRapid = TestRapid()
        startApp(testRapid, testDatabase.dataSource, javalin, "dummy")

        testRapid.sendTestMessage(behovsMelding(ident = aktørId, behovListe = """["noeannet", "hjemmel"]"""))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `legg på svar om behov nummer 2 er hjemmel, dersom første behov har en løsning`() {
        val testRapid = TestRapid()
        startApp(testRapid, testDatabase.dataSource, javalin, "dummy")

        testRapid.sendTestMessage(
            behovsMelding(
                ident = aktørId,
                behovListe = """["noeannet", "hjemmel"]""",
                løsninger = listOf("noeannet" to """{"noeannetsvar": 123}""")
            )
        )

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
        val melding = inspektør.message(0)
        assertThat(melding["hjemmel"].asText()).isEqualTo(hjemmel)
        assertThat(melding["noeannet"]["noeannetsvar"].asInt()).isEqualTo(123)
    }

    @Test
    fun `ikke legg på hjemmel-svar om svar allerede er lagt på`() {
        val testRapid = TestRapid()
        startApp(testRapid, testDatabase.dataSource, javalin, "dummy")

        testRapid.sendTestMessage(
            behovsMelding(
                behovListe = """["hjemmel"]""",
                løsninger = listOf("hjemmel" to """"svar"""")
            )
        )

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `legg på svar om første behov er måBehandleTidligereCv`() {
        val testRapid = TestRapid()
        startApp(testRapid, testDatabase.dataSource, javalin, "dummy")

        testRapid.sendTestMessage(behovsMelding(ident = aktørId, behovListe = """["måBehandleTidligereCv"]"""))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
        val message = inspektør.message(0)
        assertThat(message["måBehandleTidligereCv"].asText()).isEqualTo(måBehandleTidligereCv)
    }

    @Test
    fun `ikke legg på svar om andre uløste behov enn måBehandleTidligereCv er først i listen`() {
        val testRapid = TestRapid()
        startApp(testRapid, testDatabase.dataSource, javalin, "dummy")

        testRapid.sendTestMessage(behovsMelding(ident = aktørId, behovListe = """["noeannet", "måBehandleTidligereCv"]"""))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `legg på svar om behov nummer 2 er måBehandleTidligereCv, dersom første behov har en løsning`() {
        val testRapid = TestRapid()
        startApp(testRapid, testDatabase.dataSource, javalin, "dummy")

        testRapid.sendTestMessage(
            behovsMelding(
                ident = aktørId,
                behovListe = """["noeannet", "måBehandleTidligereCv"]""",
                løsninger = listOf("noeannet" to """{"noeannetsvar": 123}""")
            )
        )

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
        val melding = inspektør.message(0)
        assertThat(melding["måBehandleTidligereCv"].asText()).isEqualTo(måBehandleTidligereCv)
        assertThat(melding["noeannet"]["noeannetsvar"].asInt()).isEqualTo(123)
    }

    @Test
    fun `ikke legg på måBehandleTidligereCv-svar om svar allerede er lagt på`() {
        val testRapid = TestRapid()
        startApp(testRapid, testDatabase.dataSource, javalin, "dummy")

        testRapid.sendTestMessage(
            behovsMelding(
                behovListe = """["måBehandleTidligereCv"]""",
                løsninger = listOf("måBehandleTidligereCv" to """"svar"""")
            )
        )

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `legg på svar om første behov er kvp`() {
        val testRapid = TestRapid()
        startApp(testRapid, testDatabase.dataSource, javalin, "dummy")

        testRapid.sendTestMessage(behovsMelding(ident = aktørId, behovListe = """["kvp"]"""))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
        val message = inspektør.message(0)
        assertThat(message["kvp"].asText()).isEqualTo(kvp)
    }

    @Test
    fun `ikke legg på svar om andre uløste behov enn kvp er først i listen`() {
        val testRapid = TestRapid()
        startApp(testRapid, testDatabase.dataSource, javalin, "dummy")

        testRapid.sendTestMessage(behovsMelding(ident = aktørId, behovListe = """["noeannet", "kvp"]"""))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `legg på svar om behov nummer 2 er kvp, dersom første behov har en løsning`() {
        val testRapid = TestRapid()
        startApp(testRapid, testDatabase.dataSource, javalin, "dummy")

        testRapid.sendTestMessage(
            behovsMelding(
                ident = aktørId,
                behovListe = """["noeannet", "kvp"]""",
                løsninger = listOf("noeannet" to """{"noeannetsvar": 123}""")
            )
        )

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
        val melding = inspektør.message(0)
        assertThat(melding["kvp"].asText()).isEqualTo(kvp)
        assertThat(melding["noeannet"]["noeannetsvar"].asInt()).isEqualTo(123)
    }

    @Test
    fun `ikke legg på kvp-svar om svar allerede er lagt på`() {
        val testRapid = TestRapid()
        startApp(testRapid, testDatabase.dataSource, javalin, "dummy")

        testRapid.sendTestMessage(
            behovsMelding(
                behovListe = """["kvp"]""",
                løsninger = listOf("kvp" to """"svar"""")
            )
        )

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `legg på svar om første behov er arbeidsmarkedCv`() {
        val testRapid = TestRapid()
        startApp(testRapid, testDatabase.dataSource, javalin, "dummy")

        testRapid.sendTestMessage(behovsMelding(ident = aktørId, behovListe = """["arbeidsmarkedCv"]"""))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
        val message = inspektør.message(0)
        assertThat(message["arbeidsmarkedCv"].asText()).isEqualTo(arbeidsmarkedCv)
    }

    @Test
    fun `ikke legg på svar om andre uløste behov enn arbeidsmarkedCv er først i listen`() {
        val testRapid = TestRapid()
        startApp(testRapid, testDatabase.dataSource, javalin, "dummy")

        testRapid.sendTestMessage(behovsMelding(ident = aktørId, behovListe = """["noeannet", "arbeidsmarkedCv"]"""))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `legg på svar om behov nummer 2 er arbeidsmarkedCv, dersom første behov har en løsning`() {
        val testRapid = TestRapid()
        startApp(testRapid, testDatabase.dataSource, javalin, "dummy")

        testRapid.sendTestMessage(
            behovsMelding(
                ident = aktørId,
                behovListe = """["noeannet", "arbeidsmarkedCv"]""",
                løsninger = listOf("noeannet" to """{"noeannetsvar": 123}""")
            )
        )

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
        val melding = inspektør.message(0)
        assertThat(melding["arbeidsmarkedCv"].asText()).isEqualTo(arbeidsmarkedCv)
        assertThat(melding["noeannet"]["noeannetsvar"].asInt()).isEqualTo(123)
    }

    @Test
    fun `ikke legg på arbeidsmarkedCv-svar om svar allerede er lagt på`() {
        val testRapid = TestRapid()
        startApp(testRapid, testDatabase.dataSource, javalin, "dummy")

        testRapid.sendTestMessage(
            behovsMelding(
                behovListe = """["arbeidsmarkedCv"]""",
                løsninger = listOf("arbeidsmarkedCv" to """"svar"""")
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