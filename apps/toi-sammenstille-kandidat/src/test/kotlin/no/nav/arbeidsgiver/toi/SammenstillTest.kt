package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.databind.JsonNode
import no.nav.toi.TestRapid
import io.javalin.Javalin
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SammenstillTest {
    private val alleKandidatfelter = listOf(
        "arbeidsmarkedCv",
        "veileder",
        "oppfølgingsinformasjon",
        "siste14avedtak",
        "oppfølgingsperiode",
        "arenaFritattKandidatsøk",
        "hjemmel",
        "måBehandleTidligereCv",
        "kvp"
    )

    private lateinit var javalin: Javalin

    private fun kunKandidatfelter(melding: JsonNode) =
        melding.fieldNames().asSequence().toList().filter { fieldName -> alleKandidatfelter.contains(fieldName) }

    @BeforeEach
    fun before() {
        javalin = Javalin.create().start(9000)
    }

    @AfterEach
    fun after() {
        javalin.stop()
        TestDatabase().slettAlt()
    }

    @Test
    fun `Når måBehandleTidligereCv har blitt mottatt skal meldingen lagres i databasen`() {
        val aktørId = "123"
        val testRapid = TestRapid()
        val testDatabase = TestDatabase()

        startApp(testRapid, TestDatabase().dataSource, javalin, "dummy")

        testRapid.sendTestMessage(måBehandleTidligereCvMelding(aktørId))

        val lagredeKandidater = testDatabase.hentAlleKandidater()
        assertThat(lagredeKandidater.size).isEqualTo(1)
        assertThat(lagredeKandidater.first().måBehandleTidligereCv).isNotNull
    }

    @Test
    fun `Når siste14avedtak har blitt mottatt skal meldingen lagres i databasen`() {
        val aktørId = "123"
        val testRapid = TestRapid()
        val testDatabase = TestDatabase()

        startApp(testRapid, TestDatabase().dataSource, javalin, "dummy")

        testRapid.sendTestMessage(siste14avedtakMelding(aktørId))

        val lagredeKandidater = testDatabase.hentAlleKandidater()
        assertThat(lagredeKandidater.size).isEqualTo(1)
        assertThat(lagredeKandidater.first().siste14avedtak).isNotNull
    }

    @Test
    fun `Når arbeidsmarkedCv-melding har blitt mottatt skal meldingen lagres i databasen`() {
        val aktørId = "123"
        val testRapid = TestRapid()
        val testDatabase = TestDatabase()

        startApp(testRapid, TestDatabase().dataSource, javalin, "dummy")

        testRapid.sendTestMessage(arbeidsmarkedCvMelding(aktørId))

        val lagredeKandidater = testDatabase.hentAlleKandidater()
        assertThat(lagredeKandidater.size).isEqualTo(1)
        assertThat(lagredeKandidater.first().arbeidsmarkedCv).isNotNull
    }

    @Test
    fun `Når flere CV- og veiledermeldinger mottas for én kandidat skal det være én rad for kandidaten i databasen`() {
        val aktørId = "12141321"
        val testRapid = TestRapid()
        val testDatabase = TestDatabase()
        startApp(testRapid, TestDatabase().dataSource, javalin, "dummy")
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
    fun `Når kvp-melding har blitt mottatt skal meldingen lagres i databasen`() {
        val aktørId = "123"
        val testRapid = TestRapid()
        val testDatabase = TestDatabase()

        startApp(testRapid, TestDatabase().dataSource, javalin, "dummy")

        testRapid.sendTestMessage(kvp(aktørId))

        val lagredeKandidater = testDatabase.hentAlleKandidater()
        assertThat(lagredeKandidater.size).isEqualTo(1)
        assertThat(lagredeKandidater.first().kvp).isNotNull
    }
}
