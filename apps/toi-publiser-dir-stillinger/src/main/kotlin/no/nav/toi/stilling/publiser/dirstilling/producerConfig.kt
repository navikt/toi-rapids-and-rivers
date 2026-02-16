package no.nav.toi.stilling.publiser.dirstilling

import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.StringSerializer
import java.util.Properties

fun lagProducerConfig(env: Map<String, String>): Properties {
    val props = mutableMapOf<String, String>()

    props[CommonClientConfigs.CLIENT_ID_CONFIG] = "toi-publiser-dir-stillinger"
    props[CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG] = env["KAFKA_BROKERS"]!!
    props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name
    props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name

    if(!env["KAFKA_CREDSTORE_PASSWORD"].isNullOrBlank()) {
        props[SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG] = env["KAFKA_CREDSTORE_PASSWORD"]!!
        props[SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG] = env["KAFKA_CREDSTORE_PASSWORD"]!!
        props[SslConfigs.SSL_KEY_PASSWORD_CONFIG] = env["KAFKA_CREDSTORE_PASSWORD"]!!
        props[SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG] = "JKS"
        props[SslConfigs.SSL_KEYSTORE_TYPE_CONFIG] = "PKCS12"
    }

    env["KAFKA_TRUSTSTORE_PATH"]?.let { props[SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG] = it }
    env["KAFKA_KEYSTORE_PATH"]?.let { props[SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG] = it }
    env["KAFKA_TRUSTSTORE_PATH"]?.let { props[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG] = "SSL" }

    return props.toProperties()
}