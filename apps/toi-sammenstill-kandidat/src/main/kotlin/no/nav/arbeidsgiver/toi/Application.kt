package no.nav.arbeidsgiver.toi

import no.nav.helse.rapids_rivers.RapidApplication
import org.litote.kmongo.KMongo
import org.slf4j.Logger
import org.slf4j.LoggerFactory


fun startApp(repository: Repository) = RapidApplication.create(System.getenv()).also { rapid ->
    val behandler =  Behandler( repository, rapid::publish)
    VeilederLytter(rapid, behandler)
    CvLytter(rapid, behandler)
}.start()

//val mongoClient = MongoClient("mongodb://toi-sammenstill-kandidat-mongodb-2.toi-sammenstill-kandidat-mongodb:27017")
//fun main() = startApp(Repository(mongoClient))

val client = KMongo.createClient("mongodb://toi-sammenstill-kandidat-mongodb-2.toi-sammenstill-kandidat-mongodb:27017")
fun main() = startApp(Repository(client))

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)
