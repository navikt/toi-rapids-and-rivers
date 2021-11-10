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
    rapidsConnection.also { rapid ->
        val behandler = Behandler(
            Repository(datasource), rapid::publish
        )
        VeilederLytter(rapid, behandler)
        CvLytter(rapid, behandler)
    }.start()
}


fun datasource() = DatabaseKonfigurasjon(System.getenv()).lagDatasource();

fun rapidsConnection() = RapidApplication.create(System.getenv())

fun main() = startApp(datasource(), rapidsConnection())

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)
