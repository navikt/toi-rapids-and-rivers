package no.nav.arbeidsgiver.toi.arbeidssoekeropplysninger

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
class ArbeidssoekeropplysningerTest {
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

    val arbeidssokeropplysningerTopic = TopicPartition("paw.opplysninger-om-arbeidssoeker-v1", 0)

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
    fun `lesing av arbeidssøkeropplysninger fra topic lagres i database`() {
        val periodeId = UUID.randomUUID()
        val melding = melding(periodeId)
        val consumer = mockConsumer()
        val rapid = TestRapid()
        val arbeidssoekeropplysningerLytter = ArbeidssoekeropplysningerLytter({ consumer }, repository)

        produserArbeidssoekeropplysningerMelding(consumer, melding)
        arbeidssoekeropplysningerLytter.onReady(rapid)

        Thread.sleep(600)

        val periodeOpplysninger = repository.hentPeriodeOpplysninger(periodeId)

        assertThat(periodeOpplysninger).isNotNull
        assertThat(periodeOpplysninger?.helsetilstandHindrerArbeid).isFalse()
        assertThat(periodeOpplysninger?.andreForholdHindrerArbeid).isTrue()
    }

    private fun mockConsumer() = MockConsumer<Long, OpplysningerOmArbeidssoeker>(OffsetResetStrategy.EARLIEST).apply {
        schedulePollTask {
            rebalance(listOf(arbeidssokeropplysningerTopic))
            updateBeginningOffsets(mapOf(Pair(arbeidssokeropplysningerTopic, 0)))
        }
    }

    private fun produserArbeidssoekeropplysningerMelding(consumer: MockConsumer<Long, OpplysningerOmArbeidssoeker>, melding: OpplysningerOmArbeidssoeker, offset: Long = 0) {
        val record = ConsumerRecord(
            arbeidssokeropplysningerTopic.topic(),
            arbeidssokeropplysningerTopic.partition(),
            offset,
            (kotlin.random.Random(System.currentTimeMillis())).nextLong(),
            melding
        )
        consumer.schedulePollTask {
            consumer.addRecord(record)
        }
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
}
