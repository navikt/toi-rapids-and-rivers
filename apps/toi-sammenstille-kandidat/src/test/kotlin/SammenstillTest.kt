import no.nav.arbeidsgiver.toi.startApp
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SammenstillTest {

    @Test
    fun `Når veileder og CV har blitt mottatt for kandidat skal ny melding publiseres på rapid`() {
        val aktørId = "12141321"
        val testRapid = TestRapid()
        startApp(TestDatabase().dataSource, testRapid)
        testRapid.sendTestMessage(veilederMelding(aktørId, testRapid))
        testRapid.sendTestMessage(cvMelding(aktørId, testRapid))

        Thread.sleep(1000)
        val rapidInspektør = testRapid.inspektør
        assertThat(rapidInspektør.size).isEqualTo(2)

        val førsteMelding = rapidInspektør.message(0)
        assertThat(førsteMelding.get("@event_name").asText()).isEqualTo("veileder.sammenstilt")
        assertThat(førsteMelding.get("aktørId").asText()).isEqualTo(aktørId)
        assertThat(førsteMelding.has("cv")).isFalse
        assertThat(førsteMelding.get("veileder").get("aktorId").asText()).isEqualTo(aktørId)
        assertThat(førsteMelding.get("veileder").get("veilederId").asText()).isEqualTo("Z994526")
        assertThat(førsteMelding.get("veileder").get("tilordnet").asText()).isEqualTo("2020-12-21T10:58:19.023+01:00")

        val andreMelding = rapidInspektør.message(1)
        assertThat(andreMelding.get("@event_name").asText()).isEqualTo("cv.sammenstilt")
        assertThat(andreMelding.get("aktørId").asText()).isEqualTo(aktørId)
        assertThat(andreMelding.get("veileder").get("aktorId").asText()).isEqualTo(aktørId)
        assertThat(andreMelding.get("veileder").get("veilederId").asText()).isEqualTo("Z994526")
        assertThat(andreMelding.get("veileder").get("tilordnet").asText()).isEqualTo("2020-12-21T10:58:19.023+01:00")
        assertThat(andreMelding.get("cv").get("meldingstype").asText()).isEqualTo("SLETT")
        assertTrue(andreMelding.get("cv").get("oppfolgingsinformasjon").isNull)
        assertTrue(andreMelding.get("cv").get("opprettCv").isNull)
        assertTrue(andreMelding.get("cv").get("endreCv").isNull)
        assertTrue(andreMelding.get("cv").get("slettCv").isNull)
        assertTrue(andreMelding.get("cv").get("opprettJobbprofil").isNull)
        assertTrue(andreMelding.get("cv").get("endreJobbprofil").isNull)
        assertTrue(andreMelding.get("cv").get("slettJobbprofil").isNull)
        assertThat(andreMelding.get("cv").get("aktoerId").asText()).isEqualTo(aktørId)
    }

    @Test
    fun `Når cv og veileder har blitt mottatt for kandidat skal ny melding publiseres på rapid`() {
        val aktørId = "12141321"
        val testRapid = TestRapid()
        startApp(TestDatabase().dataSource, testRapid)
        testRapid.sendTestMessage(cvMelding(aktørId, testRapid))
        Thread.sleep(1000)
        testRapid.sendTestMessage(veilederMelding(aktørId, testRapid))

        Thread.sleep(1000)
        val rapidInspektør = testRapid.inspektør
        assertThat(rapidInspektør.size).isEqualTo(2)

        val førsteMelding = rapidInspektør.message(0)
        assertThat(førsteMelding.get("@event_name").asText()).isEqualTo("cv.sammenstilt")
        assertThat(førsteMelding.get("aktørId").asText()).isEqualTo(aktørId)
        assertThat(førsteMelding.has("veileder")).isFalse
        assertThat(førsteMelding.get("cv").get("meldingstype").asText()).isEqualTo("SLETT")
        assertTrue(førsteMelding.get("cv").get("oppfolgingsinformasjon").isNull)
        assertTrue(førsteMelding.get("cv").get("opprettCv").isNull)
        assertTrue(førsteMelding.get("cv").get("endreCv").isNull)
        assertTrue(førsteMelding.get("cv").get("slettCv").isNull)
        assertTrue(førsteMelding.get("cv").get("opprettJobbprofil").isNull)
        assertTrue(førsteMelding.get("cv").get("endreJobbprofil").isNull)
        assertTrue(førsteMelding.get("cv").get("slettJobbprofil").isNull)
        assertThat(førsteMelding.get("cv").get("aktoerId").asText()).isEqualTo(aktørId)

        val andreMelding = rapidInspektør.message(1)
        assertThat(andreMelding.get("aktørId").asText()).isEqualTo(aktørId)
        assertThat(andreMelding.get("@event_name").asText()).isEqualTo("veileder.sammenstilt")
        assertThat(andreMelding.get("veileder").get("aktorId").asText()).isEqualTo(aktørId)
        assertThat(andreMelding.get("veileder").get("veilederId").asText()).isEqualTo("Z994526")
        assertThat(andreMelding.get("veileder").get("tilordnet").asText()).isEqualTo("2020-12-21T10:58:19.023+01:00")
        assertThat(andreMelding.get("cv").get("meldingstype").asText()).isEqualTo("SLETT")
        assertTrue(andreMelding.get("cv").get("oppfolgingsinformasjon").isNull)
        assertTrue(andreMelding.get("cv").get("opprettCv").isNull)
        assertTrue(andreMelding.get("cv").get("endreCv").isNull)
        assertTrue(andreMelding.get("cv").get("slettCv").isNull)
        assertTrue(andreMelding.get("cv").get("opprettJobbprofil").isNull)
        assertTrue(andreMelding.get("cv").get("endreJobbprofil").isNull)
        assertTrue(andreMelding.get("cv").get("slettJobbprofil").isNull)
        assertThat(andreMelding.get("cv").get("aktoerId").asText()).isEqualTo(aktørId)
    }

    @Test
    fun `Når bare CV har blitt mottatt for kandidat skal ny melding publiseres på rapid uten veileder`() {
        val aktørId = "12141321"
        val testRapid = TestRapid()
        startApp(TestDatabase().dataSource, testRapid)
        testRapid.sendTestMessage(cvMelding(aktørId, testRapid))

        Thread.sleep(1000)
        val rapidInspektør = testRapid.inspektør
        assertThat(rapidInspektør.size).isEqualTo(1)

        val melding = rapidInspektør.message(0)
        assertThat(melding.get("@event_name").asText()).isEqualTo("cv.sammenstilt")
        assertThat(melding.get("aktørId").asText()).isEqualTo(aktørId)
        assertThat(melding.has("veileder")).isFalse
        assertThat(melding.get("cv").get("meldingstype").asText()).isEqualTo("SLETT")
        assertTrue(melding.get("cv").get("oppfolgingsinformasjon").isNull)
        assertTrue(melding.get("cv").get("opprettCv").isNull)
        assertTrue(melding.get("cv").get("endreCv").isNull)
        assertTrue(melding.get("cv").get("slettCv").isNull)
        assertTrue(melding.get("cv").get("opprettJobbprofil").isNull)
        assertTrue(melding.get("cv").get("endreJobbprofil").isNull)
        assertTrue(melding.get("cv").get("slettJobbprofil").isNull)
        assertThat(melding.get("cv").get("aktoerId").asText()).isEqualTo(aktørId)
    }

    private fun veilederMelding(aktørId: String, rapid: RapidsConnection) = """
        {
            "@event_name": "veileder",
            "aktørId": "$aktørId",
            "veileder": {
                "aktorId":"$aktørId",
                "veilederId":"Z994526",
                "tilordnet":"2020-12-21T10:58:19.023+01:00"
            }
        }
    """.trimIndent()

    private fun cvMelding(aktørId: String, rapid: RapidsConnection) = """
        {
          "aktørId": "$aktørId",
          "cv": {
            "meldingstype": "SLETT",
            "oppfolgingsinformasjon": null,
            "opprettCv": null,
            "endreCv": null,
            "slettCv": null,
            "opprettJobbprofil": null,
            "endreJobbprofil": null,
            "slettJobbprofil": null,
            "aktoerId": "$aktørId",
            "sistEndret": 1636718935.195
          },
          "@event_name": "cv"
        }
    """.trimIndent()
}