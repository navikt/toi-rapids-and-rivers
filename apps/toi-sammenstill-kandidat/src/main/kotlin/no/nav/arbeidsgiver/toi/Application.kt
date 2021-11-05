package no.nav.arbeidsgiver.toi

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClients
import com.mongodb.connection.ClusterConnectionMode
import com.mongodb.connection.ClusterType
import no.nav.helse.rapids_rivers.RapidApplication
import org.slf4j.Logger
import org.slf4j.LoggerFactory


fun startApp(repository: Repository) = RapidApplication.create(System.getenv()).also { rapid ->
    val behandler =  Behandler( repository, rapid::publish)
    VeilederLytter(rapid, behandler)
    CvLytter(rapid, behandler)
}.start()

val mongoSettings = MongoClientSettings.builder()
    .applyToClusterSettings {
        it.srvHost(System.getenv("MONGODB_HOST"))
        it.mode(ClusterConnectionMode.MULTIPLE)
        it.requiredClusterType(ClusterType.REPLICA_SET)
    }.build()

val mongoClient = MongoClients.create(mongoSettings)

fun main() = startApp(Repository(mongoClient))

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)
