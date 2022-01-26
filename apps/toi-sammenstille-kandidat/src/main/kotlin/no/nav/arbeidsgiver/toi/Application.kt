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
        val passordForRepublisering = System.getenv("PASSORD_FOR_REPUBLISERING") ?:
            throw Exception("PASSORD_FOR_REPUBLISERING kunne ikke hentes fra kubernetes secrets")

        rapidsConnection.also { rapid ->
            val repository = Repository(datasource)

            Lytter(rapid, repository, "cv")
            Lytter(rapid, repository, "veileder")
            Lytter(rapid, repository, "oppfølgingsinformasjon")
            Lytter(rapid, repository, "oppfølgingsperiode")
            Lytter(rapid, repository, "fritatt-kandidatsøk", "fritattKandidatsøk")
            Lytter(rapid, repository, "hjemmel")
            Lytter(rapid, repository, "må-behandle-tidligere-cv","måBehandleTidligereCv")

            Republiserer(passordForRepublisering, repository, rapidsConnection)
        }.start()
    } catch (t: Throwable) {
        LoggerFactory.getLogger("Applikasjon").error("Rapid-applikasjonen krasjet: ${t.message}", t)
    }
}

fun datasource() = DatabaseKonfigurasjon(System.getenv()).lagDatasource()

fun rapidsConnection() = RapidApplication.create(System.getenv())

fun main() = startApp(datasource(), rapidsConnection())

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)
