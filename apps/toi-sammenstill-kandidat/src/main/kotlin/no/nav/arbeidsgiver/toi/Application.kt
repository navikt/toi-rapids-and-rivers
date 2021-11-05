package no.nav.arbeidsgiver.toi

import com.mongodb.client.MongoClients
import no.nav.helse.rapids_rivers.RapidApplication
import org.slf4j.Logger
import org.slf4j.LoggerFactory


fun startApp(repository: Repository) = RapidApplication.create(System.getenv()).also { rapid ->
    val behandler =  Behandler( repository, rapid::publish)

    VeilederLytter(rapid, behandler)
    CvLytter(rapid, behandler)
}.start()

val mongoDbUrl = System.getenv("MONGODB_URL")
val mongoClient = MongoClients.create(mongoDbUrl)

fun main() = startApp(Repository(mongoClient))

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)