package no.nav.arbeidsgiver.toi

import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.sql.DataSource

fun startApp(
    datasource: DataSource,
    rapidsConnection: RapidsConnection
) {
    try {
        rapidsConnection.also { rapid ->
            val behandler = Behandler(
                Repository(datasource), rapid::publish
            )
            VeilederLytter(rapid, behandler)
            CvLytter(rapid, behandler)
            OppfølgingsinformasjonLytter(rapid, behandler)
        }.start()
    } catch (t: Throwable) {
        LoggerFactory.getLogger("Applikasjon").error("Rapid-applikasjonen krasjet: ${t.message}", t)
    }
}


fun datasource() = DatabaseKonfigurasjon(System.getenv()).lagDatasource();

fun rapidsConnection() = RapidApplication.create(System.getenv())

fun main() = startApp(datasource(), rapidsConnection())

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)
