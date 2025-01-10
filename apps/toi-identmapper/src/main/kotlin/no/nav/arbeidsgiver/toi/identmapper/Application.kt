package no.nav.arbeidsgiver.toi.identmapper

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.rapids_rivers.RapidApplication
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.sql.DataSource

private val env = System.getenv()

fun main() = startApp(
    env["PDL_URL"]!!,
    env["NAIS_CLUSTER_NAME"]!!,
    hentDatabasekonfigurasjon(env),
    rapidsConnection()
)

fun startApp(
    pdlUrl: String,
    cluster: String,
    dataSource: DataSource,
    rapidsConnection: RapidsConnection
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
    }.start()
}

fun rapidsConnection() = RapidApplication.create(System.getenv())

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)
