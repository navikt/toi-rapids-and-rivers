package no.nav.arbeidsgiver.toi.arbeidssoekeropplysninger

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import java.time.ZonedDateTime
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ArbeissoekeropplysningerBehovTest {

    private val testRapid = TestRapid()
    private val aktørId = "123123123"

    private val localEnv = mutableMapOf<String, String>(
        "DB_DATABASE" to "test",
        "DB_USERNAME" to "test",
        "DB_PASSWORD" to "test"
    )
    val meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val localPostgres = PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"))
        .waitingFor(Wait.forListeningPort())
        .apply { start() }
        .also { localConfig ->
            localEnv["DB_HOST"] = localConfig.host
            localEnv["DB_PORT"] = localConfig.getMappedPort(5432).toString()
        }

    lateinit var repository: Repository

    private val jacksonMapper = jacksonObjectMapper()
        .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .registerModule(JavaTimeModule())

    @BeforeAll
    fun init() {
        val databaseConfig = DatabaseConfig(localEnv, meterRegistry)
        val dataSource = databaseConfig.lagDatasource()
        kjørFlywayMigreringer(dataSource)

        repository = Repository(dataSource)
        val periode = jacksonMapper.valueToTree<JsonNode>(Periode(UUID.randomUUID(), aktørId, aktørId, ZonedDateTime.now(), null))
        repository.lagreArbeidssøkerperiodemelding(periode)
        ArbeidssoekeropplysningerBehovLytter(testRapid, repository)
        ArbeidssoekerperiodeRapidLytter(testRapid, repository)
    }

    @AfterAll
    fun teardown() {
        localPostgres.close()
    }

    @AfterEach
    fun cleanup() {
        testRapid.reset()
    }

    @Test
    fun `legg på svar om første behov er arbeidssokeropplysninger`() {
        testRapid.sendTestMessage(behovsMelding(ident = aktørId, behovListe = """["arbeidssokeropplysninger"]"""))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
        val message = inspektør.message(0)
        message.assertHarArbeidssokeropplysninger()
    }

    @Test
    fun `legg på null om det ikke finnes arbeidssokeropplysninger på person og første behov er arbeidssokeropplysninger`() {
        val aktørId = "finnes ikke i database"
        testRapid.sendTestMessage(behovsMelding(ident = aktørId, behovListe = """["arbeidssokeropplysninger"]"""))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
        val message = inspektør.message(0)
        message.assertHarArbeidssokeropplysninger()
    }

    @Test
    fun `ikke legg på svar om andre uløste behov enn arbeidssokeropplysninger er først i listen`() {
        testRapid.sendTestMessage(behovsMelding(ident = aktørId, behovListe = """["noeannet", "arbeidssokeropplysninger"]"""))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `legg på svar om behov nummer 2 er arbeidssokeropplysninger, dersom første behov har en løsning`() {
        testRapid.sendTestMessage(
            behovsMelding(
                ident = aktørId,
                behovListe = """["noeannet", "arbeidssokeropplysninger"]""",
                løsninger = listOf("noeannet" to """{"noeannetsvar": 123}""")
            )
        )

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
        val melding = inspektør.message(0)
        melding.assertHarArbeidssokeropplysninger()
        assertThat(melding["noeannet"]["noeannetsvar"].asInt()).isEqualTo(123)
    }

    @Test
    fun `ikke legg på svar om behov er en tom liste`() {
        testRapid.sendTestMessage(behovsMelding(behovListe = "[]"))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `ikke legg på svar om svar allerede er lagt på`() {
        testRapid.sendTestMessage(
            behovsMelding(
                behovListe = """["arbeidssokeropplysninger"]""",
                løsninger = listOf("arbeidssokeropplysninger" to """"svar"""")
            )
        )

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `legg på svar om behov nummer 2 er arbeidssokeropplysninger, dersom første behov har en løsning med null-verdi`() {
        testRapid.sendTestMessage(
            behovsMelding(
                ident = aktørId,
                behovListe = """["noeannet", "arbeidssokeropplysninger"]""",
                løsninger = listOf("noeannet" to "null")
            )
        )

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
        val melding = inspektør.message(0)
        melding.assertHarArbeidssokeropplysninger()
        assertThat(melding["noeannet"].isNull).isTrue
    }

    @Test
    fun `ikke legg på svar om svar allerede er lagt på med null-verdi`() {
        testRapid.sendTestMessage(
            behovsMelding(
                behovListe = """["arbeidssokeropplysninger"]""",
                løsninger = listOf("arbeidssokeropplysninger" to "null")
            )
        )

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(0)
    }
}

private fun JsonNode.assertHarArbeidssokeropplysninger() {
    assertThat(fieldNames().asSequence().toList()).contains("arbeidssokeropplysninger")
}

private fun behovsMelding(
    ident: String = "12312312312",
    behovListe: String,
    løsninger: List<Pair<String, String>> = emptyList(),
) = """
        {
            "aktørId":"$ident",
            "@behov":$behovListe
            ${løsninger.joinToString() { ""","${it.first}":${it.second}""" }}
        }
    """.trimIndent()