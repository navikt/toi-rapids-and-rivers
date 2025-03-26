package no.nav.arbeidsgiver.toi.arbeidssoekeropplysninger

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ArbeidssoekerperiodeRapidLytterTest {
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

    @BeforeAll
    fun init() {
        val databaseConfig = DatabaseConfig(localEnv, meterRegistry)
        val dataSource = databaseConfig.lagDatasource()
        kjørFlywayMigreringer(dataSource)

        repository = Repository(dataSource)
    }

    @AfterAll
    fun teardown() {
        localPostgres.close()
    }

    //@Disabled("Rapiden mottar ingen meldinger og jeg aner ikke hvorfor")
    @Test
    fun `lesing av arbeidssøkerperioder fra rapid skal lagres i database hvis aktørId er med`() {
        val periodeId = UUID.randomUUID()
        val meldingUtenAktørId = rapidPeriodeMelding(periodeId.toString())
        val meldingMedAktørId = rapidPeriodeMelding(periodeId.toString(), "123456789")
        val rapid = TestRapid()

        val arbeidssoekeropplysningerLytter = ArbeidssoekerperiodeRapidLytter(rapid, repository)
        rapid.sendTestMessage(meldingUtenAktørId)
        rapid.sendTestMessage(meldingMedAktørId)

        // Bør vurdere å bruke mock her og så heller teste all repository i egen test?
        val periodeOpplysninger = repository.hentPeriodeOpplysninger(periodeId)
        assertThat(periodeOpplysninger).isNotNull
        assertThat(periodeOpplysninger!!.identitetsnummer).isEqualTo("01010012345")
        println(periodeOpplysninger)
    }

    private fun rapidPeriodeMelding(periodeId: String, aktørId: String? = null): String = StringBuilder(
        """
        {
          "@event_name": "arbeidssokerperiode",
          "fodselsnummer": "01010012345", 
          "arbeidssokerperiode": {
            "periode_id": "$periodeId",
            "identitetsnummer": "01010012345",
            "aktørId": "jaja",
            "startet": "2025-03-07T15:08:20.582330+01:00[Europe/Oslo]"
            "avsluttet": null
          }
          """
    ).append(aktørId?.let { ", \"aktørId\": \"$it\"" } ?: "")
        .append("\n}")
        .toString().trimIndent()
}
