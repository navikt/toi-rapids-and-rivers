package no.nav.arbeidsgiver.toi.arbeidssoekeropplysninger

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RepositoryTest {
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
    lateinit var dataSource: DataSource

    @BeforeAll
    fun init() {
        val databaseConfig = DatabaseConfig(localEnv, meterRegistry)
        dataSource = databaseConfig.lagDatasource()
        kjørFlywayMigreringer(dataSource)

        repository = Repository(dataSource)
    }

    @BeforeEach
    fun tømDatabase() {
        dataSource.connection.use { conn ->
            conn.prepareStatement("truncate table periodemelding").executeUpdate()
        }
    }
    @AfterAll
    fun teardown() {
        localPostgres.close()
    }

    @Test
    fun `skal lagre arbeidssøkerperiode i database`() {
        val periodeId = UUID.randomUUID()
        val periodeMelding = periodeMeldingInnhold(periodeId)

        repository.lagreArbeidssøkerperiodemelding(periodeMelding)

        val periodeOpplysninger = repository.hentPeriodeOpplysninger(periodeId)

        assertThat(periodeOpplysninger).isNotNull
        assertThat(periodeOpplysninger?.identitetsnummer).isEqualTo("01010012345")

        val opplysningerByAktørId = repository.hentPeriodeOpplysninger("123456789")
        assertThat(opplysningerByAktørId).isNotNull
        assertThat(opplysningerByAktørId?.identitetsnummer).isEqualTo("01010012345")
    }

    @Test
    fun `skal lagre arbeidssøkerperiode i riktig rekkefølge database`() {
        val periodeId = UUID.randomUUID()
        val aktørId = UUID.randomUUID().toString()
        val periodeMeldingNy = periodeMeldingInnhold(periodeId,
            start = ZonedDateTime.now().minusMonths(1),
            slutt = ZonedDateTime.now(),
            aktørId = aktørId)

        val periodeMeldingGammel = periodeMeldingInnhold(periodeId,
            start = ZonedDateTime.now().minusMonths(2),
            slutt = null,
            aktørId = aktørId)

        repository.lagreArbeidssøkerperiodemelding(periodeMeldingNy)
        repository.lagreArbeidssøkerperiodemelding(periodeMeldingGammel)

        val periodeOpplysninger = repository.hentPeriodeOpplysninger(aktørId)

        assertThat(periodeOpplysninger).isNotNull
        assertThat(periodeOpplysninger?.periodeAvsluttet).isNotNull()
    }

    @Test
    fun `skal behandle periodeopplysninger`() {
        val periodeId = UUID.randomUUID()
        repository.lagreArbeidssøkerperiodemelding(periodeMeldingInnhold(periodeId))

        val ubehandledeOpplysninger = repository.hentUbehandledePeriodeOpplysninger()
        assertThat(ubehandledeOpplysninger.map { it.periodeId }).contains(periodeId)

        ubehandledeOpplysninger.forEach { repository.behandlePeriodeOpplysning(it.periodeId)}

        assertThat(repository.hentUbehandledePeriodeOpplysninger()).isEmpty()
    }

    private fun periodeMeldingInnhold(periodeId: UUID,
                                      start: ZonedDateTime = ZonedDateTime.parse("2025-04-07T15:00:00.0+01:00"),
                                      slutt: ZonedDateTime? = null,
                                      aktørId: String = "123456789"): JsonNode {
        val startStr = start.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        val sluttStr = slutt?.let{'"'.plus(it.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)).plus('"')} ?: "null"

        return jacksonObjectMapper().readTree("""
            {
                "periode_id": "$periodeId",
                "identitetsnummer": "01010012345",
                "aktørId": "$aktørId",
                "startet": "$startStr",
                "avsluttet": $sluttStr
              }
            """.trimIndent()
        )
    }
}


