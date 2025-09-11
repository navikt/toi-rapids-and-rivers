package no.nav.toi.stilling.indekser.eksternLytter

import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import no.nav.toi.stilling.indekser.variable
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.StringDeserializer
import java.util.*

const val stillingstopic = "toi.rekrutteringsbistand-stilling-1"

fun consumerConfig(versjon: String, env: Map<String, String>) = Properties().apply {
    val trustStorePath = env.variable("KAFKA_TRUSTSTORE_PATH")
    val keyStorePath = env.variable("KAFKA_KEYSTORE_PATH")

    put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100)
    put(ConsumerConfig.GROUP_ID_CONFIG, "rekrutteringsbistand-stilling-indekser-os-$versjon")
    put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false)
    put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java)
    put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer::class.java)

    put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, env.variable("KAFKA_BROKERS"))
    put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, env.variable("KAFKA_SCHEMA_REGISTRY"))
    put(KafkaAvroDeserializerConfig.USER_INFO_CONFIG, "${env.variable("KAFKA_SCHEMA_REGISTRY_USER")}:${env.variable("KAFKA_SCHEMA_REGISTRY_PASSWORD")}")
    put(KafkaAvroDeserializerConfig.BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO")

    put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true)
    put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL")
    put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "PKCS12")
    put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "")

    if(trustStorePath.isNotEmpty()) {
        put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, env.variable("KAFKA_TRUSTSTORE_PATH"))
        put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, env.variable("KAFKA_CREDSTORE_PASSWORD"))
    }

    if(keyStorePath.isNotEmpty()) {
        put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, env.variable("KAFKA_KEYSTORE_PATH"))
        put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, env.variable("KAFKA_CREDSTORE_PASSWORD"))
    }
}
