package no.nav.arbeidsgiver.toi

import no.nav.helse.rapids_rivers.RapidApplication
import org.slf4j.Logger
import org.slf4j.LoggerFactory


fun startApp(repository: Repository) = RapidApplication.create(System.getenv()).also { rapid ->

    val behandler =  Behandler( repository, rapid::publish)

    VeilederLytter(rapid, behandler)
    CvLytter(rapid, behandler)
}.start()

val mongoDbConnection = System.getenv("MONGODB_URL")
fun main() = startApp(Repository(mongoDbConnection))

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)