package no.nav.toi.stilling.indekser.eksternLytter

import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.confluent.kafka.serializers.KafkaAvroSerializer
import no.nav.pam.stilling.ext.avro.Ad
import no.nav.toi.stilling.indekser.configureAvroSecurityWhitelist
import org.apache.avro.Schema
import org.apache.avro.SchemaCompatibility
import org.apache.avro.SchemaNormalization
import org.apache.avro.compiler.idl.Idl
import org.apache.avro.idl.IdlReader
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.nio.file.Path.of
import java.util.UUID

class AvroStillingTopicContractTest {
    private val topic = "toi.stilling-ekstern-test"
    private val schemaRegistryUrl = "mock://stilling-ekstern"
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
    fun `avro-melding fra topic wireformat skal kunne leses og konverteres`() {
        val originalMelding = ad(UUID.randomUUID().toString())

        val serialisert = serializer.serialize(topic, originalMelding)
        val deserialisert = deserializer.deserialize(topic, serialisert) as Ad

        assertThat(deserialisert.uuid).isEqualTo(originalMelding.uuid)
        assertThat(deserialisert.title).isEqualTo(originalMelding.title)
        assertThat(konverterTilStilling(deserialisert).uuid.toString()).isEqualTo(originalMelding.uuid)
    }

    @Test
    fun `avro-schema for topicmelding skal være kompatibelt med avdl-kontrakten`() {
        val schemaFraGenerertKlasse = Ad.getClassSchema()
        val schemaFraAvdl = IdlReader().parse(of("src/main/avro/StillingEkstern.avdl")).protocol.getType("Ad")

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
