package no.nav.arbeidsgiver.toi.arbeidssoekeropplysninger

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.flywaydb.core.Flyway
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.sql.DataSource

fun main() = RapidApplication.create(System.getenv()).apply {
    val meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    val databaseConfig = DatabaseConfig(System.getenv(), meterRegistry)
    val dataSource = databaseConfig.lagDatasource()
    kjørFlywayMigreringer(dataSource)

    val repository = Repository(dataSource)

    val consumer = { KafkaConsumer<Long, OpplysningerOmArbeidssoeker>(consumerConfig) }

    val arbeidssokerperioderRapidLytter = ArbeidssoekerperiodeRapidLytter(this, repository)
    val arbeidssoekeropplysningerLytter = ArbeidssoekeropplysningerLytter(consumer, repository)
    register(arbeidssoekeropplysningerLytter)
}.start()

fun kjørFlywayMigreringer(dataSource: DataSource) {
    Flyway.configure()
        .loggers("slf4j")
        .dataSource(dataSource)
        .load()
        .migrate()
}

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)
