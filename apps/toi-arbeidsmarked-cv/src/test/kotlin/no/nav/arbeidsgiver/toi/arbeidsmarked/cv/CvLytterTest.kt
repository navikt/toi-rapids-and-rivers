package no.nav.arbeidsgiver.toi.arbeidsmarked.cv

import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.confluent.kafka.serializers.KafkaAvroSerializer
import no.nav.arbeid.cv.avro.*
import no.nav.toi.TestRapid
import org.apache.avro.SchemaCompatibility
import org.apache.avro.SchemaNormalization
import org.apache.avro.idl.IdlReader
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.MockConsumer
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.common.TopicPartition
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path.of
import java.time.Instant
import java.time.LocalDate

class CvLytterTest {

    val cvTopic = TopicPartition("teampam.cv-endret-ekstern-v2", 0)
    val schemaRegistryUrl = "mock://arbeidsmarked-cv"
    lateinit var serializer: KafkaAvroSerializer
    lateinit var deserializer: KafkaAvroDeserializer

    val behandleCv: (Melding) -> ArbeidsmarkedCv = { melding ->
        ArbeidsmarkedCv(melding)
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
    fun `lesing av cv-meldinger fra topic skal publiseres på rapid`() {
        val melding = melding()
        val consumer = mockConsumer()
        val rapid = TestRapid()
        val cvLytter = CvLytter({ consumer }, behandleCv)

        produserCvMelding(consumer, melding)
        cvLytter.onReady(rapid)

        val inspektør = ventPåPublisering(rapid)
        Assertions.assertThat(inspektør.size).isEqualTo(1)

        val meldingJson = inspektør.message(0)

        Assertions.assertThat(meldingJson.propertyNames().asSequence().toList()).containsExactlyInAnyOrder(
            "@event_name",
            "arbeidsmarkedCv",
            "aktørId",
            "system_read_count",
            "@id",
            "@opprettet",
            "system_participating_services"
        )

        Assertions.assertThat(meldingJson.get("aktørId")).isNotNull
    }

    @Test
    fun `avro-melding fra topic wireformat skal kunne leses og publiseres paa rapid`() {
        val originalMelding = melding()
        val serialisert = serializer.serialize(cvTopic.topic(), originalMelding)
        val deserialisert = deserializer.deserialize(cvTopic.topic(), serialisert) as Melding
        Assertions.assertThat(deserialisert.opprettCv.cv.foedselsdato).isEqualTo(originalMelding.opprettCv.cv.foedselsdato)

        val consumer = mockConsumer()
        val rapid = TestRapid()
        val cvLytter = CvLytter({ consumer }, behandleCv)

        produserCvMelding(consumer, deserialisert)
        cvLytter.onReady(rapid)

        val inspektør = ventPåPublisering(rapid)
        val meldingJson = inspektør.message(0)

        Assertions.assertThat(inspektør.size).isEqualTo(1)
        Assertions.assertThat(meldingJson["aktørId"].asString()).isEqualTo(originalMelding.aktoerId)
    }

    @Test
    fun `avro-schema for topicmelding skal være kompatibelt med avdl-kontrakten`() {
        val schemaFraGenerertKlasse = Melding.getClassSchema()
        val schemaFraAvdl = IdlReader().parse(of("src/main/avro/Cv.avdl")).protocol.getType("Melding")

        Assertions.assertThat(
            SchemaNormalization.parsingFingerprint64(schemaFraGenerertKlasse)
        ).isEqualTo(SchemaNormalization.parsingFingerprint64(schemaFraAvdl))

        Assertions.assertThat(
            SchemaCompatibility.checkReaderWriterCompatibility(schemaFraGenerertKlasse, schemaFraAvdl).type
        ).isEqualTo(SchemaCompatibility.SchemaCompatibilityType.COMPATIBLE)
        Assertions.assertThat(
            SchemaCompatibility.checkReaderWriterCompatibility(schemaFraAvdl, schemaFraGenerertKlasse).type
        ).isEqualTo(SchemaCompatibility.SchemaCompatibilityType.COMPATIBLE)
    }

    private fun mockConsumer() = MockConsumer<String, Melding>(OffsetResetStrategy.EARLIEST).apply {
        schedulePollTask {
            rebalance(listOf(cvTopic))
            updateBeginningOffsets(mapOf(Pair(cvTopic, 0)))
        }
    }

    private fun produserCvMelding(consumer: MockConsumer<String, Melding>, cv: Melding, offset: Long = 0) {
        val record = ConsumerRecord(
            cvTopic.topic(),
            cvTopic.partition(),
            offset,
            cv.aktoerId,
            cv
        )
        consumer.schedulePollTask {
            consumer.addRecord(record)
        }
    }

    private fun ventPåPublisering(rapid: TestRapid): TestRapid.RapidInspector {
        var inspektør: TestRapid.RapidInspector
        val startTime = Instant.now()
        do {
            inspektør = rapid.inspektør
            Thread.sleep(50)
            if (Instant.now().isAfter(startTime.plusSeconds(30))) {
                throw AssertionError("Forventet at minst 1 melding skulle være publisert på rapid innen et halvt minutt")
            }
        } while (inspektør.size < 1)
        return inspektør
    }


    private fun melding() = Melding().apply {
        meldingstype = Meldingstype.OPPRETT
        aktoerId = "123"
        sistEndret = Instant.now()
        opprettCv = opprettCv()
    }

    private fun opprettCv() = OpprettCv().apply { cv = cv() }
    private fun cv() = Cv().apply() {
        cvId = "cv-123"
        aktoerId = "123"
        sistEndret = Instant.now()
        opprettet = Instant.now()
        fodselsnummer = "11111111111"
        foedselsdato = LocalDate.of(1992, 1, 11)
        fornavn = "Test"
        etternavn = "Testesen"
        sammendrag = ""
        synligForArbeidsgiver = true
        synligForVeileder = true
        val foererkortErvervetDato = LocalDate.of(2010, 11, 5)
        foererkort = Foererkort(
            listOf(
                FoererkortKlasse(
                    "B",
                    "Førerkort klasse B",
                    foererkortErvervetDato,
                    foererkortErvervetDato.plusYears(80)
                )
            )
        )
        arbeidserfaring = emptyList()
        utdannelse = emptyList()
        fagdokumentasjon = emptyList()
        godkjenninger = emptyList()
        kurs = emptyList()
        sertifikat = emptyList()
        annenErfaring = emptyList()
        spraakferdigheter = emptyList()
    }
}
