import no.nav.arbeidsgiver.toi.ontologitjeneste.OntologiLytter
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection

fun main(): Unit = startApp(ontologiUrl(), RapidApplication.create(System.getenv()))

fun startApp(ontologiUrl: String, rapidsConnection: RapidsConnection) = rapidsConnection.also {
    OntologiLytter(ontologiUrl, rapidsConnection)
}.start()

private fun ontologiUrl() = System.getenv("ONTOLOGI_URL") ?: throw Exception("Mangler ONTOLOGI_URL")