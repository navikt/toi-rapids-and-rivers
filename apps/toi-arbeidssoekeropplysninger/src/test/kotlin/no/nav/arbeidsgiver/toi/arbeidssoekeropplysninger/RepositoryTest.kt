package no.nav.arbeidsgiver.toi.arbeidssoekeropplysninger

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.api.v1.*
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v2.Annet
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.MockConsumer
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.common.TopicPartition
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import java.time.Instant
import java.util.*

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

    @Test
    fun `skal lagre arbeidssøkeropplysninger i database`() {
        val periodeId = UUID.randomUUID()
        val melding = melding(periodeId)

        repository.lagreArbeidssøkeropplysninger(melding)

        val periodeOpplysninger = repository.hentPeriodeOpplysninger(periodeId)

        assertThat(periodeOpplysninger).isNotNull
        assertThat(periodeOpplysninger?.helsetilstandHindrerArbeid).isFalse()
        assertThat(periodeOpplysninger?.andreForholdHindrerArbeid).isTrue()
    }

    @Test
    fun `skal lagre liste med arbeidssøkeropplysninger i database`() {
        val meldinger = listOf(
            melding(UUID.randomUUID()),
            melding(UUID.randomUUID())
        )

        repository.lagreArbeidssøkeropplysninger(meldinger)

        meldinger.forEach { m ->
            val periodeOpplysninger = repository.hentPeriodeOpplysninger(m.periodeId)

            assertThat(periodeOpplysninger).isNotNull
            assertThat(periodeOpplysninger?.helsetilstandHindrerArbeid).isFalse()
            assertThat(periodeOpplysninger?.andreForholdHindrerArbeid).isTrue()
        }
    }


    @Test
    fun `skal lagre arbeidssøkerperiode i database`() {
        val periodeId = UUID.randomUUID()
        val periodeMelding = periodeMelding(periodeId)

        repository.lagreOppfølgingsperiodemelding(periodeMelding)

        val periodeOpplysninger = repository.hentPeriodeOpplysninger(periodeId)

        assertThat(periodeOpplysninger).isNotNull
        assertThat(periodeOpplysninger?.helsetilstandHindrerArbeid).isNull()
        assertThat(periodeOpplysninger?.identitetsnummer).isEqualTo("01010012345")
    }

    @Test
    fun `skal lagre arbeidssøkerperiode i database og oppdatere med opplysninger`() {
        val periodeId = UUID.randomUUID()
        val periodeMelding = periodeMelding(periodeId)
        val opplysningerMelding = melding(periodeId)

        repository.lagreOppfølgingsperiodemelding(periodeMelding)
        repository.lagreArbeidssøkeropplysninger(opplysningerMelding)

        val periodeOpplysninger = repository.hentPeriodeOpplysninger(periodeId)

        assertThat(periodeOpplysninger).isNotNull
        assertThat(periodeOpplysninger?.helsetilstandHindrerArbeid).isFalse()
        assertThat(periodeOpplysninger?.andreForholdHindrerArbeid).isTrue()
        assertThat(periodeOpplysninger?.identitetsnummer).isEqualTo("01010012345")
    }


    @Test
    fun `skal lagre arbeidssøkeropplysninger i database og oppdatere med arbeidssøkerperiode`() {
        val periodeId = UUID.randomUUID()
        val periodeMelding = periodeMelding(periodeId)
        val opplysningerMelding = melding(periodeId)

        repository.lagreArbeidssøkeropplysninger(opplysningerMelding)
        repository.lagreOppfølgingsperiodemelding(periodeMelding)

        val periodeOpplysninger = repository.hentPeriodeOpplysninger(periodeId)

        assertThat(periodeOpplysninger).isNotNull
        assertThat(periodeOpplysninger?.helsetilstandHindrerArbeid).isFalse()
        assertThat(periodeOpplysninger?.andreForholdHindrerArbeid).isTrue()
        assertThat(periodeOpplysninger?.identitetsnummer).isEqualTo("01010012345")
    }

    @Test
    fun `skal behandle periodeopplysninger`() {
        val periodeId = UUID.randomUUID()
        val periodeMelding = periodeMelding(periodeId)
        val opplysningerMelding = melding(periodeId)

        repository.lagreArbeidssøkeropplysninger(opplysningerMelding)
        repository.lagreOppfølgingsperiodemelding(periodeMelding)

        val ubehandledeOpplysninger = repository.hentUbehandledePeriodeOpplysninger()
        assertThat(ubehandledeOpplysninger.map { it.periodeId }).contains(periodeId)

        ubehandledeOpplysninger.forEach { repository.behandlePeriodeOpplysning(it.periodeId)}

        assertThat(repository.hentUbehandledePeriodeOpplysninger()).isEmpty()
    }

    private fun melding(periodeId: UUID) = OpplysningerOmArbeidssoeker.newBuilder()
        .setId(UUID.randomUUID())
        .setPeriodeId(periodeId)
        .setAnnet(Annet.newBuilder()
            .setAndreForholdHindrerArbeid(JaNeiVetIkke.JA)
            .build()
        )
        .setHelse(Helse.newBuilder()
            .setHelsetilstandHindrerArbeid(JaNeiVetIkke.VET_IKKE)
            .build()
        )
        .setSendtInnAv(Metadata.newBuilder()
            .setUtfoertAv(Bruker.newBuilder().setId("junit").setType(BrukerType.SYSTEM).build())
            .setKilde("junit")
            .setTidspunkt(Instant.now())
            .setAarsak("test")
            .build()
        )
        .setJobbsituasjon(Jobbsituasjon.newBuilder()
            .setBeskrivelser(emptyList())
            .build()
        )
        .build()

    private fun periodeMelding(periodeId: UUID): JsonNode = jacksonObjectMapper().readTree("""
        {
          "@event_name": "arbeidssokerperiode",
          "id": "$periodeId",
          "identitetsnummer": "01010012345",
          "startet": "2025-03-07T15:08:20.582330+01:00[Europe/Oslo]",
          "avsluttet": null
          }
        """.trimIndent()
    )
}


