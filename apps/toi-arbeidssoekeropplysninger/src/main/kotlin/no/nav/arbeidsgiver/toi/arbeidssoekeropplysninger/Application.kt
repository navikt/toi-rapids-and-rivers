package no.nav.arbeidsgiver.toi.arbeidssoekeropplysninger

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.flywaydb.core.Flyway
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.http.HttpClient
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

    val httpClient: HttpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.ALWAYS)
        .version(HttpClient.Version.HTTP_1_1)
        .build()
    val leaderElector = LeaderElector(System.getenv("ELECTOR_PATH"), httpClient = httpClient)
    val publiserOpplysningerJobb = PubliserOpplysningerJobb(repository, this, leaderElector, meterRegistry)
    if (System.getenv("PUBLISER_TIL_RAPID_ENABLED").equals("enabled", ignoreCase = true)) {
        publiserOpplysningerJobb.start()
    }

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
