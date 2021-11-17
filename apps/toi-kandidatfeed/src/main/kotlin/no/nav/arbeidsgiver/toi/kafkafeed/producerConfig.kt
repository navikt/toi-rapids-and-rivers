package no.nav.arbeidsgiver.toi.kafkafeed

import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.StringSerializer

val producerConfig = mapOf(
    CommonClientConfigs.CLIENT_ID_CONFIG to "toi-kandidatfeed",
    CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG to System.getenv("KAFKA_BROKERS"),
    ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java.name,
    ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java.name,
    CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to "SSL",
    SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG to System.getenv("KAFKA_KEYSTORE_PATH"),
    SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG to System.getenv("KAFKA_CREDSTORE_PASSWORD"),
    SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG to System.getenv("KAFKA_CREDSTORE_PASSWORD"),
    SslConfigs.SSL_KEY_PASSWORD_CONFIG to System.getenv("KAFKA_CREDSTORE_PASSWORD"),
    SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG to "JKS",
    SslConfigs.SSL_KEYSTORE_TYPE_CONFIG to "PKCS12",
    SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG to System.getenv("KAFKA_TRUSTSTORE_PATH"),
).toProperties()
