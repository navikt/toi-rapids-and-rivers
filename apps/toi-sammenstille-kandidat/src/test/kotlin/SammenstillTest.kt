import com.fasterxml.jackson.databind.JsonNode
import no.nav.arbeidsgiver.toi.startApp
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class SammenstillTest {
    private val alleKandidatfelter = listOf(
        "cv",
        "veileder",
        "oppfølgingsinformasjon",
        "oppfølgingsperiode",
        "fritattKandidatsøk",
    )

    private fun kunKandidatfelter(melding: JsonNode) = melding.fieldNames().asSequence().toList().filter{ fieldName -> alleKandidatfelter.contains(fieldName) }

    @Test
    fun `Når veileder og CV har blitt mottatt for kandidat skal ny melding publiseres på rapid`() {
        val aktørId = "12141321"
        val testRapid = TestRapid()
        startApp(TestDatabase().dataSource, testRapid)
        testRapid.sendTestMessage(veilederMelding(aktørId))
        testRapid.sendTestMessage(cvMelding(aktørId))

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
        testRapid.sendTestMessage(cvMelding(aktørId))
        testRapid.sendTestMessage(veilederMelding(aktørId))

        val rapidInspektør = testRapid.inspektør
        assertThat(rapidInspektør.size).isEqualTo(2)

        val førsteMelding = rapidInspektør.message(0)
        assertThat(førsteMelding.get("@event_name").asText()).isEqualTo("cv.sammenstilt")
        assertThat(førsteMelding.get("aktørId").asText()).isEqualTo(aktørId)
        assertThat(kunKandidatfelter(førsteMelding)).containsExactlyInAnyOrder("cv")

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
        testRapid.sendTestMessage(cvMelding(aktørId))

        val rapidInspektør = testRapid.inspektør
        assertThat(rapidInspektør.size).isEqualTo(1)

        val melding = rapidInspektør.message(0)
        assertThat(melding.get("@event_name").asText()).isEqualTo("cv.sammenstilt")
        assertThat(melding.get("aktørId").asText()).isEqualTo(aktørId)
        assertThat(kunKandidatfelter(melding)).containsExactlyInAnyOrder("cv")

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

    @Test
    fun `Når oppfølgingsinformasjon har blitt mottatt for kandidat skal ny melding publiseres på rapid`() {
        val aktørId = "12141321"
        val testRapid = TestRapid()
        startApp(TestDatabase().dataSource, testRapid)
        testRapid.sendTestMessage(oppfølgingsinformasjonMelding(aktørId))

        val rapidInspektør = testRapid.inspektør
        assertThat(rapidInspektør.size).isEqualTo(1)

        val melding = rapidInspektør.message(0)
        assertThat(melding.get("@event_name").asText()).isEqualTo("oppfølgingsinformasjon.sammenstilt")
        assertThat(melding.get("aktørId").asText()).isEqualTo(aktørId)
        assertThat(kunKandidatfelter(melding)).containsExactlyInAnyOrder("oppfølgingsinformasjon")

        val oppfølgingsinformasjonPåMelding = melding.get("oppfølgingsinformasjon")
        assertThat(oppfølgingsinformasjonPåMelding.get("formidlingsgruppe").asText()).isEqualTo("IARBS")
        assertThat(oppfølgingsinformasjonPåMelding.get("iservFraDato").isNull).isTrue
        assertThat(oppfølgingsinformasjonPåMelding.get("fornavn").asText()).isEqualTo("TULLETE")
        assertThat(oppfølgingsinformasjonPåMelding.get("etternavn").asText()).isEqualTo("TABBE")
        assertThat(oppfølgingsinformasjonPåMelding.get("oppfolgingsenhet").asText()).isEqualTo("0318")
        assertThat(oppfølgingsinformasjonPåMelding.get("kvalifiseringsgruppe").asText()).isEqualTo("BATT")
        assertThat(oppfølgingsinformasjonPåMelding.get("rettighetsgruppe").asText()).isEqualTo("AAP")
        assertThat(oppfølgingsinformasjonPåMelding.get("hovedmaal").asText()).isEqualTo("BEHOLDEA")
        assertThat(oppfølgingsinformasjonPåMelding.get("sikkerhetstiltakType").isNull).isTrue
        assertThat(oppfølgingsinformasjonPåMelding.get("diskresjonskode").isNull).isTrue
        assertThat(oppfølgingsinformasjonPåMelding.get("harOppfolgingssak").asBoolean()).isTrue
        assertThat(oppfølgingsinformasjonPåMelding.get("sperretAnsatt").asBoolean()).isFalse
        assertThat(oppfølgingsinformasjonPåMelding.get("erDoed").asBoolean()).isFalse
        assertThat(oppfølgingsinformasjonPåMelding.get("doedFraDato").isNull).isTrue
        assertThat(oppfølgingsinformasjonPåMelding.get("sistEndretDato").asText()).isEqualTo("2020-10-30T14:15:38+01:00")
    }

    @Test
    fun `Når oppfølgingsperiode har blitt mottatt for kandidat skal ny melding publiseres på rapid`() {
        val aktørId = "12141321"
        val testRapid = TestRapid()
        startApp(TestDatabase().dataSource, testRapid)
        testRapid.sendTestMessage(oppfølgingsperiodeMelding(aktørId))

        val rapidInspektør = testRapid.inspektør
        assertThat(rapidInspektør.size).isEqualTo(1)

        val melding = rapidInspektør.message(0)
        assertThat(melding.get("@event_name").asText()).isEqualTo("oppfølgingsperiode.sammenstilt")
        assertThat(melding.get("aktørId").asText()).isEqualTo(aktørId)
        assertThat(kunKandidatfelter(melding)).containsExactlyInAnyOrder("oppfølgingsperiode")

        val oppfølgingsperiodePåMelding = melding.get("oppfølgingsperiode")

        assertThat(oppfølgingsperiodePåMelding.get("uuid").asText()).isEqualTo("0b0e2261-343d-488e-a70f-807f4b151a2f")
        assertThat(oppfølgingsperiodePåMelding.get("aktorId").asText()).isEqualTo(aktørId)
        assertThat(oppfølgingsperiodePåMelding.get("startDato").asText()).isEqualTo("2021-12-01T18:18:00.435004+01:00")
        assertThat(oppfølgingsperiodePåMelding.get("sluttDato").isNull).isTrue
    }

    @Test
    fun `Når fritattKandidatsøk har blitt mottatt for kandidat skal ny melding publiseres på rapid`() {
        val aktørId = "123"
        val testRapid = TestRapid()

        startApp(TestDatabase().dataSource, testRapid)
        testRapid.sendTestMessage(fritattKandidatsøkMelding(aktørId, true))

        val rapidInspektør = testRapid.inspektør
        assertThat(rapidInspektør.size).isEqualTo(1)

        val melding = rapidInspektør.message(0)
        assertThat(melding.get("@event_name").asText()).isEqualTo("fritatt-kandidatsøk.sammenstilt")
        assertThat(melding.get("aktørId").asText()).isEqualTo(aktørId)
        assertThat(kunKandidatfelter(melding)).containsExactlyInAnyOrder("fritattKandidatsøk")

        val fritattKandidatsøkPåMelding = melding.get("fritattKandidatsøk")
        assertThat(fritattKandidatsøkPåMelding.get("fritattKandidatsok").asBoolean()).isTrue
    }

    @Test
    fun `Når flere CV- og veiledermeldinger mottas for én kandidat skal det være én rad for kandidaten i databasen`() {
        val aktørId = "12141321"
        val testRapid = TestRapid()
        val testDatabase = TestDatabase()
        startApp(testDatabase.dataSource, testRapid)
        testRapid.sendTestMessage(cvMelding(aktørId))
        testRapid.sendTestMessage(veilederMelding(aktørId))
        testRapid.sendTestMessage(cvMelding(aktørId))
        testRapid.sendTestMessage(cvMelding(aktørId))
        testRapid.sendTestMessage(veilederMelding(aktørId))
        testRapid.sendTestMessage(veilederMelding(aktørId))

        val antallLagredeKandidater = testDatabase.hentAntallKandidater()
        assertThat(antallLagredeKandidater).isEqualTo(1)
    }

    @Test
    fun `Objekter inne i sammenstiltKandidat-melding skal ikke være stringified`() {
        val testRapid = TestRapid()
        startApp(TestDatabase().dataSource, testRapid)
        testRapid.sendTestMessage(oppfølgingsinformasjonMelding("12141321"))

        val rapidInspektør = testRapid.inspektør
        assertThat(rapidInspektør.size).isEqualTo(1)

        val melding = rapidInspektør.message(0)
        val oppfølgingsinformasjonPåMelding = melding.get("oppfølgingsinformasjon")
        assertThat(oppfølgingsinformasjonPåMelding.isObject).isTrue
    }

    @Test
    fun `metadata fra opprinnelig melding skal ikke fjernes når sammenstilleren publiserer ny melding på rapid`() {
        val testRapid = TestRapid()
        startApp(TestDatabase().dataSource, testRapid)
        testRapid.sendTestMessage(cvMeldingMedSystemParticipatingServices())

        val rapidInspektør = testRapid.inspektør
        assertThat(rapidInspektør.size).isEqualTo(1)

        val melding = rapidInspektør.message(0)

        val felterPåMelding = melding.fieldNames().asSequence().toList()
        assertThat(felterPåMelding).contains("system_participating_services")
        assertThat(felterPåMelding).contains("system_read_count")
    }

    private fun veilederMelding(aktørId: String) = """
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

    private fun cvMelding(aktørId: String) = """
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

    private fun cvMeldingMedSystemParticipatingServices() = """
        {
          "aktørId": "123",
          "cv": {
            "meldingstype": "SLETT",
            "oppfolgingsinformasjon": null,
            "opprettCv": null,
            "endreCv": null,
            "slettCv": null,
            "opprettJobbprofil": null,
            "endreJobbprofil": null,
            "slettJobbprofil": null,
            "aktoerId": "123",
            "sistEndret": 1636718935.195
          },
          "@event_name": "cv",
          "system_participating_services": [
            {
              "service": "toi-cv",
              "instance": "toi-cv-58849d5f86-7qffs",
              "time": "2021-11-19T10:53:59.163725026"
            }
          ]
        }
    """.trimIndent()

    private fun oppfølgingsinformasjonMelding(aktørId: String) = """
        {
            "aktørId": "$aktørId",
            "fodselsnummer": "12345678912",
            "@event_name": "oppfølgingsinformasjon",
            "oppfølgingsinformasjon": {
                "fodselsnummer": "12345678912",
                "formidlingsgruppe": "IARBS",
                "iservFraDato": null,
                "fornavn": "TULLETE",
                "etternavn": "TABBE",
                "oppfolgingsenhet": "0318",
                "kvalifiseringsgruppe": "BATT",
                "rettighetsgruppe": "AAP",
                "hovedmaal": "BEHOLDEA",
                "sikkerhetstiltakType": null,
                "diskresjonskode": null,
                "harOppfolgingssak": true,
                "sperretAnsatt": false,
                "erDoed": false,
                "doedFraDato": null,
                "sistEndretDato": "2020-10-30T14:15:38+01:00"
            }   
        }
    """.trimIndent()

    private fun oppfølgingsperiodeMelding(aktørId: String) = """
        {
            "aktørId": "$aktørId",
            "@event_name": "oppfølgingsperiode",
            "oppfølgingsperiode": {
                "uuid": "0b0e2261-343d-488e-a70f-807f4b151a2f",
                "aktorId": "$aktørId",
                "startDato": "2021-12-01T18:18:00.435004+01:00",
                "sluttDato": null 
            }   
        }
    """.trimIndent()

    private fun fritattKandidatsøkMelding(aktørId: String, fritattKandidatsøk: Boolean) = """
        {
            "aktørId": "$aktørId",
            "fodselsnummer": "123",
            "@event_name": "fritatt-kandidatsøk",
            "fritattKandidatsøk": {
                "fritattKandidatsok": $fritattKandidatsøk
            }
        }
    """.trimIndent()
}
