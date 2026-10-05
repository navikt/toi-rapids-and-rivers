package no.nav.arbeidsgiver.toi.livshendelser

import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.confluent.kafka.serializers.KafkaAvroSerializer
import no.nav.person.pdl.leesah.Personhendelse
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

class AvroPersonhendelseTopicContractTest {
    private val topic = "pdl.leesah-v1"
    private val schemaRegistryUrl = "mock://personhendelse"
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
        val originalMelding = personhendelse()

        val serialisert = serializer.serialize(topic, originalMelding)
        val deserialisert = deserializer.deserialize(topic, serialisert) as Personhendelse

        assertThat(deserialisert.hendelseId).isEqualTo(originalMelding.hendelseId)
        assertThat(deserialisert.opplysningstype).isEqualTo(originalMelding.opplysningstype)
        assertThat(deserialisert.personidenter).containsExactlyElementsOf(originalMelding.personidenter)
    }

    @Test
    fun `avro-schema for topicmelding skal være kompatibelt med avdl-kontrakten`() {
        val schemaFraGenerertKlasse = Personhendelse.getClassSchema()
        val schemaFraAvdl = IdlReader().parse(of("src/main/avro/Personhendelse.avdl")).protocol.getType("Personhendelse")

        assertThat(SchemaNormalization.parsingFingerprint64(schemaFraGenerertKlasse))
            .isEqualTo(SchemaNormalization.parsingFingerprint64(schemaFraAvdl))
        assertThat(
            SchemaCompatibility.checkReaderWriterCompatibility(schemaFraGenerertKlasse, schemaFraAvdl).type
        ).isEqualTo(SchemaCompatibility.SchemaCompatibilityType.COMPATIBLE)
        assertThat(
            SchemaCompatibility.checkReaderWriterCompatibility(schemaFraAvdl, schemaFraGenerertKlasse).type
        ).isEqualTo(SchemaCompatibility.SchemaCompatibilityType.COMPATIBLE)
    }

    private fun lesSchemaFraAvdl(recordNavn: String): Schema {
        val avdlPath = Path.of("src", "main", "avro", "Personhendelse.avdl")
        Idl(avdlPath.toFile()).use { idl ->
            return idl.CompilationUnit().getType(recordNavn)
        }
        IdlReader().parse(of("src/main/avro/Personhendelse.avdl")).protocol.getType("Aktor")
    }
}
