package no.nav.arbeidsgiver.toi.arbeidssoekerperiode

import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.confluent.kafka.serializers.KafkaAvroSerializer
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.toi.TestRapid
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.MockConsumer
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.common.TopicPartition
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.*

class ArbeidssoekerperiodeTest {
    val arbeidssokerperioderTopic = TopicPartition("paw.arbeidssokerperioder-v1", 0)
    val schemaRegistryUrl = "mock://arbeidssoekerperiode"
    lateinit var serializer: KafkaAvroSerializer
    lateinit var deserializer: KafkaAvroDeserializer

    val behandleMelding: (Periode) -> ArbeidssokerPeriode = { melding ->
        ArbeidssokerPeriode(melding, SimpleMeterRegistry())
    }

    @BeforeEach
    fun setUp() {
        configureAvroSecurityWhitelist()

        val mockSchemaRegistryClient = MockSchemaRegistryClient()
        serializer = KafkaAvroSerializer(mockSchemaRegistryClient).apply {
            configure(
                mapOf("schema.registry.url" to schemaRegistryUrl),
                false
            )
        }
        deserializer = KafkaAvroDeserializer(mockSchemaRegistryClient).apply {
            configure(
                mapOf(
                    "schema.registry.url" to schemaRegistryUrl,
                    KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG to true
                ),
                false
            )
        }
    }

    @Test
    fun `lesing av arbeidssøkerperioder fra topic skal publiseres på rapid`() {
        val melding = melding()
        val consumer = mockConsumer()
        val rapid = TestRapid()
        val arbeidssoekerperiodeLytter = ArbeidssoekerperiodeLytter({ consumer }, behandleMelding)

        produserArbeidssoekerperiodeMelding(consumer, melding)
        arbeidssoekerperiodeLytter.onReady(rapid)

        rapid.waitForFirstMessage()
        val inspektør = rapid.inspektør
        assertThat(inspektør.size).isEqualTo(1)

        val meldingJson = inspektør.message(0)

        assertThat(meldingJson.propertyNames()).containsExactlyInAnyOrder(
            "@event_name",
            "fodselsnummer",
            "arbeidssokerperiode",
            "@id",
            "@opprettet",
            "system_read_count",
            "system_participating_services"
        )

        assertThat(meldingJson.get("fodselsnummer")).isNotNull
    }

    @Test
    fun `avro-melding fra topic wireformat skal kunne leses og publiseres paa rapid`() {
        val originalMelding = melding()
        val serialisert = serializer.serialize(arbeidssokerperioderTopic.topic(), originalMelding)
        val deserialisert = deserializer.deserialize(arbeidssokerperioderTopic.topic(), serialisert) as Periode
        assertThat(deserialisert.identitetsnummer).isEqualTo(originalMelding.identitetsnummer)

        val consumer = mockConsumer()
        val rapid = TestRapid()
        val arbeidssoekerperiodeLytter = ArbeidssoekerperiodeLytter({ consumer }, behandleMelding)

        produserArbeidssoekerperiodeMelding(consumer, deserialisert)
        arbeidssoekerperiodeLytter.onReady(rapid)

        rapid.waitForFirstMessage()
        val inspektør = rapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
        assertThat(inspektør.message(0)["fodselsnummer"].asText()).isEqualTo(originalMelding.identitetsnummer)
    }

    private fun mockConsumer() = MockConsumer<Long, Periode>(OffsetResetStrategy.EARLIEST).apply {
        schedulePollTask {
            rebalance(listOf(arbeidssokerperioderTopic))
            updateBeginningOffsets(mapOf(Pair(arbeidssokerperioderTopic, 0)))
        }
    }

    private fun produserArbeidssoekerperiodeMelding(
        consumer: MockConsumer<Long, Periode>,
        melding: Periode,
        offset: Long = 0
    ) {
        val record = ConsumerRecord(
            arbeidssokerperioderTopic.topic(),
            arbeidssokerperioderTopic.partition(),
            offset,
            (kotlin.random.Random(System.currentTimeMillis())).nextLong(),
            melding
        )
        consumer.schedulePollTask {
            consumer.addRecord(record)
        }
    }

    private fun melding() = Periode.newBuilder()
        .setId(UUID.randomUUID())
        .setStartet(
            Metadata.newBuilder()
                .setAarsak("whatever")
                .setKilde("junit")
                .setTidspunkt(Instant.now())
                .setUtfoertAv(Bruker.newBuilder().setId("jeje").setType(BrukerType.SYSTEM).build())
                .build()
        )
        .setAvsluttet(null)
        .setIdentitetsnummer("01010012345")
        .build()
}
