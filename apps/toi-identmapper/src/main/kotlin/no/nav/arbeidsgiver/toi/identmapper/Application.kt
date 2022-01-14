package no.nav.arbeidsgiver.toi.identmapper

import no.nav.arbeidsgiver.toi.cv.pdlKafkaConsumerConfig
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.person.pdl.aktor.v2.Aktor
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.sql.DataSource

private fun pdlConsumerConfig() = pdlKafkaConsumerConfig(System.getenv())
private fun pdlConsumer() = KafkaConsumer<String, Aktor>(pdlConsumerConfig())
private val env = System.getenv()

fun main() = startApp(
    env["PDL_URL"]!!,
    env["NAIS_CLUSTER_NAME"]!!,
    hentDatabasekonfigurasjon(env),
    rapidsConnection(),
    pdlConsumer()
)

fun startApp(
    pdlUrl: String,
    cluster: String,
    dataSource: DataSource,
    rapidsConnection: RapidsConnection,
    pdlConsumer: Consumer<String, Aktor>
) {
    rapidsConnection.also {
        val accessTokenClient = AccessTokenClient(env)

        val pdlKlient = PdlKlient(pdlUrl, accessTokenClient)
        val repository = Repository(dataSource)
        val aktørIdCache = AktorIdCache(repository, cluster == "dev-gcp", pdlKlient::hentAktørId)

        rapidsConnection.register(object: RapidsConnection.StatusListener {
            override fun onStartup(rapidsConnection: RapidsConnection) {
                repository.kjørFlywayMigreringer()
            }
        })

        listOf("fnr", "fodselsnr", "fodselsnummer").forEach { fnrKey ->
            Lytter(fnrKey, rapidsConnection, cluster, aktørIdCache::hentAktørId)
        }

        // TODO: Skru på PDL-topic
        // rapidsConnection.register(PdlLytter(pdlConsumer, repository::lagreAktørId))
    }.start()
}

fun rapidsConnection() = RapidApplication.create(System.getenv())

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)
