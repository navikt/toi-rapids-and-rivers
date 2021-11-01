package no.nav.arbeidsgiver.toi

import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.serialization.StringDeserializer

object Configuration {
    val cvTopic = "arbeid-pam-cv-endret-v6"
}

fun cvLytterConfig(envs: Map<String, String>) = mapOf<String, String>(
    ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to (envs["KAFKA_BROKERS"] ?: throw Exception("KAFKA_BROKERS er ikke definert")),
    ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java.canonicalName,
    ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to KafkaAvroDeserializer::class.java.canonicalName,
    ConsumerConfig.GROUP_ID_CONFIG to (envs["ARBEIDSPLASSEN_CV_KAFKA_GROUP"] ?: throw Exception("ARBEIDSPLASSEN_CV_KAFKA_GROUP er ikke definert")),
    ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",

    CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to "SASL_SSL",
    SaslConfigs.SASL_MECHANISM to "PLAIN",
    SaslConfigs.SASL_JAAS_CONFIG to "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"srv-toi-cv\" password=\"${envs["TOI_CV_SERVICEBRUKER_PASSORD"]}\";",

    KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG to "true",
    KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG to (envs["KAFKA_SCHEMA_REGISTRY"] ?: throw Exception("KAFKA_SCHEMA_REGISTRY er ikke definert")),
    KafkaAvroDeserializerConfig.BASIC_AUTH_CREDENTIALS_SOURCE to "USER_INFO",
    KafkaAvroDeserializerConfig.USER_INFO_CONFIG to "${envs["KAFKA_SCHEMA_REGISTRY_USER"]!!}:${envs["KAFKA_SCHEMA_REGISTRY_PASSWORD"]!!}"
).toProperties()