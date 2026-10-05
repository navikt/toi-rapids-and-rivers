package no.nav.arbeidsgiver.toi.identmapper

import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.confluent.kafka.serializers.KafkaAvroSerializer
import no.nav.person.pdl.aktor.v2.Aktor
import no.nav.person.pdl.aktor.v2.Identifikator
import no.nav.person.pdl.aktor.v2.Type
import org.apache.avro.SchemaCompatibility
import org.apache.avro.SchemaNormalization
import org.apache.avro.idl.IdlReader
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path.of

class AvroAktorTopicContractTest {
    private val topic = "pdl.aktor-v2"
    private val schemaRegistryUrl = "mock://aktor-v2"
    private lateinit var serializer: KafkaAvroSerializer
    private lateinit var deserializer: KafkaAvroDeserializer

    @BeforeEach
    fun setUp() {
        configureAvroSecurityWhitelist()
        val mockSchemaRegistryClient = MockSchemaRegistryClient()
        serializer = KafkaAvroSerializer(mockSchemaRegistryClient).apply {
            configure(mapOf("schema.registry.url" to schemaRegistryUrl), false)
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
    fun `avro-melding fra topic wireformat skal kunne leses`() {
        val originalMelding = Aktor(
            listOf(
                Identifikator("12345678910", Type.FOLKEREGISTERIDENT, true),
                Identifikator("1000000000001", Type.AKTORID, true)
            )
        )

        val serialisert = serializer.serialize(topic, originalMelding)
        val deserialisert = deserializer.deserialize(topic, serialisert) as Aktor

        assertThat(deserialisert.identifikatorer).hasSize(2)
        assertThat(deserialisert.identifikatorer.first().idnummer)
            .isEqualTo(originalMelding.identifikatorer.first().idnummer)
    }

    @Test
    fun `avro-schema for topicmelding skal være kompatibelt med avdl-kontrakten`() {
        val schemaFraGenerertKlasse = Aktor.getClassSchema()
        val schemaFraAvdl = IdlReader().parse(of("src/main/avro/AktorV2.avdl")).protocol.getType("Aktor")

        assertThat(SchemaNormalization.parsingFingerprint64(schemaFraGenerertKlasse))
            .isEqualTo(SchemaNormalization.parsingFingerprint64(schemaFraAvdl))
        assertThat(
            SchemaCompatibility.checkReaderWriterCompatibility(schemaFraGenerertKlasse, schemaFraAvdl).type
        ).isEqualTo(SchemaCompatibility.SchemaCompatibilityType.COMPATIBLE)
        assertThat(
            SchemaCompatibility.checkReaderWriterCompatibility(schemaFraAvdl, schemaFraGenerertKlasse).type
        ).isEqualTo(SchemaCompatibility.SchemaCompatibilityType.COMPATIBLE)
    }

}
