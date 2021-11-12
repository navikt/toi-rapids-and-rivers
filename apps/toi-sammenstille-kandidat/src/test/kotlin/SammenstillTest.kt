import no.nav.arbeidsgiver.toi.startApp
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SammenstillTest {

    @Test
    fun `Når veileder og CV har blitt mottatt for kandidat skal ny melding publiseres på rapid`() { //TODO: bedre navn
        val aktørId = "10000100000"
        val testRapid = TestRapid()
        startApp(TestDatabase.dataSource, testRapid)
        testRapid.sendTestMessage(veilederMelding(aktørId, testRapid))
        testRapid.sendTestMessage(cvMelding(aktørId, testRapid))

        Thread.sleep(1000)
        val rapidInspektør = testRapid.inspektør
        assertThat(rapidInspektør.size).isEqualTo(2)

        val veilederExpected = """
            {
              "@event_name": "veileder",
              "aktørId": "10000100000",
              "veileder": {
                "aktorId": "1000001002586",
                "veilederId": "Z994526",
                "tilordnet": "2020-12-21T10:58:19.023+01:00"
              },
              "system_read_count": 0
            }
        """.trimIndent()
        assertThat(rapidInspektør.message(0).get("@event_name").asText()).isEqualTo("veileder.sammenstilt")
        assertThat(rapidInspektør.message(0).get("cv")).isNull()
        assertThat(rapidInspektør.message(0).get("veileder").asText()).isEqualToIgnoringWhitespace(veilederExpected)

        assertThat(rapidInspektør.message(1).get("@event_name").asText()).isEqualTo("cv.sammenstilt")
        assertThat(rapidInspektør.message(1).get("veileder").asText()).isEqualToIgnoringWhitespace(veilederExpected)
        assertThat(rapidInspektør.message(1).get("cv").asText()).isEqualToIgnoringWhitespace(
            """
            {
              "meldingstype": "SLETT",
              "oppfolgingsinformasjon": null,
              "opprettCv": null,
              "endreCv": null,
              "slettCv": null,
              "opprettJobbprofil": null,
              "endreJobbprofil": null,
              "slettJobbprofil": null,
              "aktoerId": "10000100000",
              "sistEndret": 1.636718935195E9
            }
        """.trimIndent()
        )
    }

    private fun veilederMelding(aktørId: String, rapid: RapidsConnection) = """
        {
            "@event_name": "veileder",
            "aktørId": "$aktørId",
            "veileder": {
                "aktorId":"1000001002586",
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