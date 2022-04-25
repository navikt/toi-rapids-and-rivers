package no.nav.arbeidsgiver.toi.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.authentication
import io.javalin.Javalin
import no.nav.arbeidsgiver.toi.Repository
import no.nav.arbeidsgiver.toi.TestDatabase
import no.nav.arbeidsgiver.toi.Testdata
import no.nav.arbeidsgiver.toi.startApp
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SynlighetTest {

    private lateinit var javalin: Javalin

    @BeforeEach
    fun beforeEach() {
        javalin = Javalin.create {
            it.defaultContentType = "application/json"
            //it.accessManager(styrTilgang(issuerProperties))
        }.start(8301)
    }

    @AfterEach
    fun afterEach() {
        javalin.stop()
    }

    @Test
    fun `kall med tom liste skal returnere tomt resultat`() {
        val objectmapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
        val repository = Repository(TestDatabase().dataSource)
        val rapid = TestRapid()

        startApp(repository, javalin, rapid)

        rapid.sendTestMessage(Testdata.komplettHendelseSomFørerTilSynlighetTrue())
        Assertions.assertThat(rapid.inspektør.size).isEqualTo(1)

        val kandidater = emptyList<String>()
        val jsonString = objectmapper.writeValueAsString(kandidater)

        val response = Fuel.post("http://localhost:8301/synlighet")
            .body(jsonString)
            .response().second

        Assertions.assertThat(response.statusCode).isEqualTo(200)
        Assertions.assertThat(response.body().asString("application/json")).isEqualTo("{}")
    }

    @Test
    fun `Person som er synlig skal returneres som synlig`(){
        val objectmapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
        val repository = Repository(TestDatabase().dataSource)
        val rapid = TestRapid()

        startApp(repository, javalin, rapid)

        rapid.sendTestMessage(Testdata.komplettHendelseSomFørerTilSynlighetTrue())
        Assertions.assertThat(rapid.inspektør.size).isEqualTo(1)

        val kandidater = listOf("12345678912")
        val jsonString = objectmapper.writeValueAsString(kandidater)

        val response = Fuel.post("http://localhost:8301/synlighet")
            .body(jsonString)
            .response().second

        Assertions.assertThat(response.statusCode).isEqualTo(200)

        val expectedJsonResponse = objectmapper.writeValueAsString(mapOf("12345678912" to true))
        Assertions.assertThat(response.body().asString("application/json")).isEqualTo(expectedJsonResponse)

    }


}