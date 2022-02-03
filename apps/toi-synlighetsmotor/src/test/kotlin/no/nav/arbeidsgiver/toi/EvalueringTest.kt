package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.github.kittinunf.fuel.Fuel
import io.javalin.Javalin
import no.nav.arbeidsgiver.toi.Testdata.Companion.komplettHendelseSomFørerTilSynlighetTrue
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EvalueringTest {
    private lateinit var javalin: Javalin

    @BeforeEach
    fun beforeEach() {
        javalin = Javalin.create().start(9000)
    }

    @AfterEach
    fun afterEach() {
        javalin.stop()
    }

    @Test
    fun `Synlighetsevaluering som følge av melding skal lagres på personen i databasen`() {
        val repository = Repository(TestDatabase().dataSource)

        testProgramMedHendelse(
            komplettHendelseSomFørerTilSynlighetTrue(),
            enHendelseErPublisertMedSynlighetsverdiOgFerdigBeregnet(
                synlighet = true,
                ferdigBeregnet = true
            ),
            repository
        )

        val evalueringFraDb = repository.hentMedAktørid(aktorId = "123456789")
        Assertions.assertThat(evalueringFraDb).isEqualTo(
            evalueringMedAltTrue()
        )
    }

    @Test
    fun `GET mot evalueringsendepunkt skal returnere 200 OK med evaluering på oppgitt fødselsnummer`() {
        val objectmapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
        val repository = Repository(TestDatabase().dataSource)
        val rapid = TestRapid()

        startApp(repository, javalin, rapid)

        rapid.sendTestMessage(komplettHendelseSomFørerTilSynlighetTrue())
        Assertions.assertThat(rapid.inspektør.size).isEqualTo(1)

        val response = Fuel.get("http://localhost:9000/evaluering/12345678912").response().second
        Assertions.assertThat(response.statusCode).isEqualTo(200)

        val responseJson = response.body().asString("application/json")
        val responeEvaluering = objectmapper.readValue(responseJson, Evaluering::class.java)
        Assertions.assertThat(responeEvaluering).isEqualTo(evalueringMedAltTrue())
    }

    @Test
    fun `GET mot evalueringsendepunkt skal returnere 404 for fødselsnummer som ikke finnes i databasen`() {
        val repository = Repository(TestDatabase().dataSource)
        val rapid = TestRapid()

        startApp(repository, javalin, rapid)

        Assertions.assertThat(rapid.inspektør.size).isEqualTo(0)

        val response = Fuel.get("http://localhost:9000/evaluering/12345678912").response().second
        Assertions.assertThat(response.statusCode).isEqualTo(404)
    }
}

private fun evalueringMedAltTrue() = Evaluering(
    harAktivCv = true,
    harJobbprofil = true,
    harSettHjemmel = true,
    maaIkkeBehandleTidligereCv = true,
    erIkkefritattKandidatsøk = true,
    erUnderOppfoelging = true,
    harRiktigFormidlingsgruppe = true,
    erIkkeKode6eller7 = true,
    erIkkeSperretAnsatt = true,
    erIkkeDoed = true,
    erFerdigBeregnet = true
)
