import no.nav.arbeidsgiver.toi.ontologitjeneste.OntologiLytter
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun main(): Unit = startApp(ontologiUrl(), RapidApplication.create(System.getenv()))

fun startApp(ontologiUrl: String, rapidsConnection: RapidsConnection) = rapidsConnection.also {
    OntologiLytter(ontologiUrl, rapidsConnection)
}.start()

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)

private fun ontologiUrl() = System.getenv("ONTOLOGI_URL") ?: throw Exception("Mangler ONTOLOGI_URL")