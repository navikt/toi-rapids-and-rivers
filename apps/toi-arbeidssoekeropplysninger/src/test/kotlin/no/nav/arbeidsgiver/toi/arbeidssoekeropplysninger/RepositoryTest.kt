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
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
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
    fun `skal kun hente nyeste arbeidssøkerperiode for aktørid`() {
        val meldinger = listOf(
            melding(UUID.randomUUID()),
            melding(UUID.randomUUID()),
            melding(UUID.randomUUID())
        )

        repository.lagreArbeidssøkeropplysninger(meldinger)

        val aktørId = UUID.randomUUID().toString()
        repository.lagreOppfølgingsperiodemelding(periodeMeldingInnhold(meldinger[0].periodeId,
            ZonedDateTime.now().minusMonths(10), aktørId = aktørId))
        repository.lagreOppfølgingsperiodemelding(periodeMeldingInnhold(meldinger[1].periodeId,
            ZonedDateTime.now().minusMonths(5), ZonedDateTime.now().minusMonths(2), aktørId = aktørId))
        val nyesteId = repository.lagreOppfølgingsperiodemelding(periodeMeldingInnhold(meldinger[2].periodeId,
            ZonedDateTime.now().minusMonths(1), aktørId = aktørId))

        val periodeOpplysninger = repository.hentPeriodeOpplysninger(aktørId)
        assertThat(nyesteId).isEqualTo(periodeOpplysninger?.id)

        val ubehandlede = repository.hentUbehandledePeriodeOpplysninger().filter { it.aktørId == aktørId }
        assertThat(ubehandlede.map { it.id }).containsOnly(nyesteId)
    }

    @Test
    fun `skal lagre arbeidssøkerperiode i database`() {
        val periodeId = UUID.randomUUID()
        val periodeMelding = periodeMeldingInnhold(periodeId)

        repository.lagreOppfølgingsperiodemelding(periodeMelding)

        val periodeOpplysninger = repository.hentPeriodeOpplysninger(periodeId)

        assertThat(periodeOpplysninger).isNotNull
        assertThat(periodeOpplysninger?.helsetilstandHindrerArbeid).isNull()
        assertThat(periodeOpplysninger?.identitetsnummer).isEqualTo("01010012345")

        val opplysningerByAktørId = repository.hentPeriodeOpplysninger("123456789")
        assertThat(opplysningerByAktørId).isNotNull
        assertThat(opplysningerByAktørId?.identitetsnummer).isEqualTo("01010012345")
    }

    @Test
    fun `skal lagre arbeidssøkerperiode i database og oppdatere med opplysninger`() {
        val periodeId = UUID.randomUUID()
        val periodeMelding = periodeMeldingInnhold(periodeId)
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
        val periodeMelding = periodeMeldingInnhold(periodeId)
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
        repository.lagreOppfølgingsperiodemelding(periodeMeldingInnhold(periodeId))

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

    private fun periodeMelding(periodeId: UUID): JsonNode = jacksonObjectMapper().readTree("""
        {
          "@event_name": "arbeidssokerperiode",
          "arbeidssokerperiode": ${periodeMeldingInnhold(periodeId)},
          "aktørId": "123456789",
          "fodselsnummer": "01010012345",
          "@id": "whatever"
          }
        """.trimIndent()
    )
}


