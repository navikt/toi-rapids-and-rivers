package no.nav.arbeidsgiver.toi.arenafritattkandidatsok

import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun main() = RapidApplication.create(System.getenv()).also { rapidsConnection ->
    val dataSource = DatabaseKonfigurasjon(System.getenv()).lagDatasource()
    val repository = FritattRepository(dataSource)

    rapidsConnection.register(object : RapidsConnection.StatusListener {
        override fun onStartup(rapidsConnection: RapidsConnection) {
            repository.flywayMigrate(dataSource)
        }
    })

    ArenaFritattKandidatsokLytter(rapidsConnection, repository)
}.start()


val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)
